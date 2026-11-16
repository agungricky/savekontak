package com.example.wacontactcollector.data

object PhoneNormalizer {
    /** Returns Indonesian mobile numbers as 628xxxxxxxxxx, or null for invalid input. */
    fun normalize(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        val normalized = when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("8") -> "62$digits"
            else -> return null
        }
        return normalized.takeIf { it.matches(Regex("^628\\d{8,12}$")) }
    }
}
