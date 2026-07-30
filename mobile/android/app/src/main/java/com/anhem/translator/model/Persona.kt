package com.anhem.translator.model

import kotlinx.serialization.Serializable

@Serializable
data class Persona(
    val id: String,
    val label: String,
    val emoji: String,
    val rel: String,
    val tGender: String,
    val tAge: String,
    val tone: String,
    val olderSib: Boolean? = null,
    val cDesc: String? = null
) {
    companion object {
        val ALL = listOf(
            Persona("girlfriend", "女友", "💗", "lover", "female", "younger", ""),
            Persona("sister", "姐姐", "👩", "custom", "female", "older", "casual", olderSib = true, cDesc = "姐姐"),
            Persona("sis", "妹妹", "👧", "custom", "female", "younger", "casual", olderSib = false, cDesc = "妹妹"),
            Persona("brother", "哥哥", "👨", "custom", "male", "older", "casual", olderSib = true, cDesc = "哥哥"),
            Persona("bro", "弟弟", "👦", "custom", "male", "younger", "casual", olderSib = false, cDesc = "弟弟"),
            Persona("friend", "朋友", "💬", "friend", "male", "same", "casual"),
            Persona("client_m", "客户(男)", "🤝", "client", "male", "older", "formal"),
            Persona("client_f", "客户(女)", "🤝", "client", "female", "older", "formal"),
        )

        val DEFAULT = ALL[0] // girlfriend
    }
}

@Serializable
data class Profile(val gender: String = "male", val age: Int? = null)

@Serializable
data class CustomContext(val me: String = "", val them: String = "", val desc: String = "")

@Serializable
data class Context(
    val rel: String = "lover",
    val tGender: String = "female",
    val tAge: String = "younger",
    val custom: CustomContext = CustomContext()
)

@Serializable
data class Settings(
    val apiKey: String = "",
    val dialect: String = "north",
    val glossary: String = "",
    val clipboardMonitorEnabled: Boolean = true
)

@Serializable
data class UsageStats(
    val reqs: Int = 0,
    val tokens: Long = 0,
    val cny: Double = 0.0
)

@Serializable
data class TranslationCard(
    val dir: String,
    val src: String,
    val out: String,
    val note: String = "",
    val err: Boolean = false,
    val costCny: Double? = null,
    val costTokens: Long? = null
)
