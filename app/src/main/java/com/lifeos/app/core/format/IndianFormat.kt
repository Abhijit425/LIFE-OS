package com.lifeos.app.core.format

import kotlin.math.abs

/**
 * Indian number formatting (lakh/crore grouping) done by hand so output is identical on every
 * device regardless of ICU/locale data: 1234567.5 -> "12,34,567.50".
 */
object IndianFormat {

    /** Groups an unsigned integer string: last 3 digits, then pairs. "1234567" -> "12,34,567". */
    fun groupDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = ArrayDeque<String>()
        while (rest.length > 2) {
            parts.addFirst(rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.addFirst(rest)
        return parts.joinToString(",") + "," + last3
    }

    /** ₹ amount from paise. [showPaise]=false rounds half-up to whole rupees. */
    fun rupees(paise: Long, showPaise: Boolean = true): String {
        val negative = paise < 0
        val absPaise = abs(paise)
        val body = if (showPaise) {
            val r = absPaise / 100
            val p = absPaise % 100
            groupDigits(r.toString()) + "." + p.toString().padStart(2, '0')
        } else {
            groupDigits(((absPaise + 50) / 100).toString())
        }
        return (if (negative) "-₹" else "₹") + body
    }

    /** Compact form for tight UI: ₹950, ₹12.5K, ₹3.2L, ₹1.4Cr. */
    fun rupeesCompact(paise: Long): String {
        val negative = paise < 0
        val r = abs(paise) / 100.0
        val text = when {
            r >= 1_00_00_000 -> trim(r / 1_00_00_000) + "Cr"
            r >= 1_00_000 -> trim(r / 1_00_000) + "L"
            r >= 1_000 -> trim(r / 1_000) + "K"
            else -> groupDigits(Math.round(r).toString())
        }
        return (if (negative) "-₹" else "₹") + text
    }

    private fun trim(v: Double): String {
        val rounded = Math.round(v * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
