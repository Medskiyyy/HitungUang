package com.hitunguang.feature.recurring.domain.model

/** Stored in recurring_transactions.recurrence_rule as "DAILY", "WEEKLY" or "MONTHLY:<day>". */
sealed interface RecurrenceRule {
    fun encode(): String

    data object Daily : RecurrenceRule {
        override fun encode() = "DAILY"
    }

    /** Repeats on the weekday of the first occurrence. */
    data object Weekly : RecurrenceRule {
        override fun encode() = "WEEKLY"
    }

    /** Months without [dayOfMonth] use their last day; later months go back to [dayOfMonth]. */
    data class Monthly(val dayOfMonth: Int) : RecurrenceRule {
        init {
            require(dayOfMonth in 1..31) { "Tanggalnya harus 1–31." }
        }

        override fun encode() = "MONTHLY:$dayOfMonth"
    }

    companion object {
        fun parse(value: String): RecurrenceRule = when {
            value == "DAILY" -> Daily
            value == "WEEKLY" -> Weekly
            value.startsWith("MONTHLY:") -> Monthly(
                value.removePrefix("MONTHLY:").toIntOrNull()
                    ?: throw IllegalArgumentException("Aturan ulang nggak dikenal: $value")
            )
            else -> throw IllegalArgumentException("Aturan ulang nggak dikenal: $value")
        }
    }
}
