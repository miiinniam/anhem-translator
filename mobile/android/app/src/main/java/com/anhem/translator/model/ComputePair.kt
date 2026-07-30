package com.anhem.translator.model

fun computePair(profile: Profile, ctx: Context): PronounPair {
    val g = profile.gender
    return when {
        ctx.rel == "custom" -> PronounPair(ctx.custom.me.ifEmpty { "AI定" }, ctx.custom.them.ifEmpty { "AI定" })
        ctx.rel == "lover" -> if (g == "male") PronounPair("anh", "em") else PronounPair("em", "anh")
        ctx.rel == "elder" -> PronounPair("cháu", if (ctx.tGender == "male") "chú/bác" else "cô/bác")
        ctx.rel == "stranger" -> PronounPair("tôi", "bạn")
        ctx.rel == "friend" -> PronounPair("mình", "cậu")
        ctx.rel == "client" -> PronounPair(
            if (ctx.tAge == "older") "em" else "tôi",
            if (ctx.tGender == "male") "anh" else "chị"
        )
        ctx.tAge == "older" -> PronounPair("em", if (ctx.tGender == "male") "anh" else "chị")
        ctx.tAge == "younger" -> PronounPair(if (g == "male") "anh" else "chị", "em")
        else -> PronounPair("mình", "cậu")
    }
}

data class PronounPair(val me: String, val them: String)
