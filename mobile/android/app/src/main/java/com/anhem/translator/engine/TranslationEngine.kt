package com.anhem.translator.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object TranslationEngine {
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val API_BASE = "https://api.deepseek.com"
    private const val MODEL = "deepseek-v4-flash"
    private val PRICE = mapOf("hit" to 0.5, "miss" to 2.0, "out" to 8.0)

    data class TranslationResult(
        val text: String,
        val note: String = "",
        val costCny: Double? = null,
        val costTokens: Long? = null
    )

    data class StreamEvent(val token: String? = null, val usage: JsonObject? = null, val done: Boolean = false)

    /** Streaming translation — emits tokens via Flow */
    fun translateStream(
        text: String, dir: String, persona: com.anhem.translator.model.Persona,
        profile: com.anhem.translator.model.Profile, settings: com.anhem.translator.model.Settings,
        tone: String
    ): Flow<StreamEvent> = flow {
        val messages = PromptBuilder.buildMessages(text, dir, persona, profile, settings, tone)
        val body = buildJsonObject {
            put("model", MODEL)
            put("messages", JsonArray(messages.map { msg ->
                buildJsonObject {
                    put("role", msg["role"]!!)
                    put("content", msg["content"]!!)
                }
            }))
            put("temperature", if (dir == "zh2vi") 0.2 else 0.1)
            put("frequency_penalty", 0.15)
            put("max_tokens", 2000)
            put("stream", true)
            put("stream_options", buildJsonObject { put("include_usage", true) })
        }

        val request = Request.Builder()
            .url("$API_BASE/chat/completions")
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            val msg = when (response.code) {
                401 -> "API Key 无效"
                402 -> "余额不足"
                403 -> "权限不足"
                429 -> "请求过于频繁请稍后"
                500 -> "服务器错误"
                503 -> "服务暂时不可用"
                else -> "HTTP ${response.code}"
            }
            throw IOException(msg)
        }

        val reader = response.body?.source() ?: throw IOException("无返回内容")

        try {
            while (true) {
                val line = reader.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(StreamEvent(done = true))
                    break
                }
                try {
                    val obj = json.parseToJsonElement(data).jsonObject
                    val choices = obj["choices"]?.jsonArray
                    if (choices != null && choices.isNotEmpty()) {
                        val delta = choices[0].jsonObject["delta"]?.jsonObject
                        val content = delta?.get("content")?.let {
                            if (it is JsonNull) null else it.jsonPrimitive.content
                        }
                        if (content != null) emit(StreamEvent(token = content))
                    }
                    val usage = obj["usage"]?.jsonObject
                    if (usage != null) emit(StreamEvent(usage = usage))
                } catch (_: Exception) {
                    // Skip malformed JSON chunks
                }
            }
        } finally {
            response.close()
        }
    }

    /** Synchronous translation — returns complete result */
    suspend fun translate(
        text: String, dir: String, persona: com.anhem.translator.model.Persona,
        profile: com.anhem.translator.model.Profile, settings: com.anhem.translator.model.Settings,
        tone: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        val messages = PromptBuilder.buildMessages(text, dir, persona, profile, settings, tone)
        val body = buildJsonObject {
            put("model", MODEL)
            put("messages", JsonArray(messages.map { msg ->
                buildJsonObject {
                    put("role", msg["role"]!!)
                    put("content", msg["content"]!!)
                }
            }))
            put("temperature", if (dir == "zh2vi") 0.2 else 0.1)
            put("frequency_penalty", 0.15)
            put("max_tokens", 2000)
            put("stream", false)
        }

        val request = Request.Builder()
            .url("$API_BASE/chat/completions")
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        val response = client.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                val msg = when (response.code) {
                    401 -> "API Key 无效"; 402 -> "余额不足"; 403 -> "权限不足"
                    429 -> "请求过于频繁请稍后"; 500 -> "服务器错误"; 503 -> "服务暂时不可用"
                    else -> "HTTP ${response.code}"
                }
                throw IOException(msg)
            }
            val raw = response.body?.string() ?: throw IOException("无返回内容")
            val obj = json.parseToJsonElement(raw).jsonObject
            val content = obj["choices"]?.jsonArray?.get(0)?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: ""
            val parts = content.split(Regex("\\n\\s*-{3,}\\s*\\n"))
            val out = parts[0].trim()
            val note = if (parts.size > 1) parts[1].trim() else ""

            val usage = obj["usage"]?.jsonObject
            val cost = if (usage != null) calcCost(usage) else null

            TranslationResult(out, note, cost?.first, cost?.second)
        } finally {
            response.close()
        }
    }

    private fun calcCost(usage: JsonObject): Pair<Double, Long> {
        val hit = usage["prompt_cache_hit_tokens"]?.jsonPrimitive?.longOrNull ?: 0
        val promptTokens = usage["prompt_tokens"]?.jsonPrimitive?.longOrNull ?: 0
        val miss = (usage["prompt_cache_miss_tokens"]?.jsonPrimitive?.longOrNull) ?: (promptTokens - hit)
        val out = usage["completion_tokens"]?.jsonPrimitive?.longOrNull ?: 0
        val cny = (hit * PRICE["hit"]!! + miss * PRICE["miss"]!! + out * PRICE["out"]!!) / 1_000_000.0
        val totalTokens = promptTokens + out
        return Pair(cny, totalTokens)
    }
}
