package com.hitunguang.core.common.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    private val locale = Locale("in", "ID")
    private val numberFormatter = NumberFormat.getIntegerInstance(locale)

    fun format(amount: Double, showSign: Boolean = false): String {
        val isNegative = amount < 0
        val formatted = numberFormatter.format(abs(amount))
        return when {
            isNegative -> "-Rp $formatted"
            showSign -> "+Rp $formatted"
            else -> "Rp $formatted"
        }
    }

    fun format(amount: Long, showSign: Boolean = false): String {
        val isNegative = amount < 0
        val formatted = numberFormatter.format(abs(amount))
        return when {
            isNegative -> "-Rp $formatted"
            showSign -> "+Rp $formatted"
            else -> "Rp $formatted"
        }
    }
}
