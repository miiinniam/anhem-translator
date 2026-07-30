package com.anhem.translator.model

fun detectDirection(text: String): String {
    var cjk = 0
    var total = 0
    for (ch in text) {
        val code = ch.code
        if ((code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in 0xF900..0xFAFF)) cjk++
        total++
    }
    if (total == 0) return "zh2vi"
    val ratio = cjk.toDouble() / total
    return when {
        ratio > 0.5 -> "zh2vi"
        ratio < 0.15 -> "vi2zh"
        else -> "zh2vi"
    }
}
