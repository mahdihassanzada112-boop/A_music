package com.amusic.core.common

import java.nio.charset.Charset

private val DIACRITICS = Regex("[\\u064B-\\u065F\\u0670\\u0640]")

/** Folds Arabic/Persian letter variants, digits, diacritics and half-spaces for searching. */
fun normalizeForSearch(text: String): String {
    val folded = StringBuilder(text.length)
    for (c in text.replace(DIACRITICS, "")) {
        folded.append(
            when (c) {
                'ي', 'ى', 'ئ' -> 'ی'
                'ك' -> 'ک'
                'أ', 'إ', 'آ' -> 'ا'
                'ة', 'ۀ' -> 'ه'
                'ؤ' -> 'و'
                '‌', '‏', '‎' -> ' '
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c.lowercaseChar()
            },
        )
    }
    return folded.toString().trim().replace(Regex("\\s+"), " ")
}

/**
 * Old ID3 tags often hold Windows-1256 (Persian/Arabic) bytes that were decoded as Latin-1.
 * Repairs such text; anything that does not clearly match that pattern is returned unchanged.
 */
fun repairLegacyArabic(text: String): String {
    if (text.any { it.code > 0xFF }) return text
    val letters = text.count { it.isLetter() || it.code >= 0x80 }
    val high = text.count { it.code >= 0xC0 }
    // Accented Latin words have few high characters; mis-decoded Arabic is almost all high.
    if (high < 2 || high * 10 < letters * 7) return text
    val decoded = String(text.toByteArray(Charsets.ISO_8859_1), Charset.forName("windows-1256"))
    val arabic = decoded.count { it in '؀'..'ۿ' }
    return if (arabic * 10 >= high * 7) decoded else text
}
