package net.cofcool.sourcebox.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun isHabitRequiredOnDate(
    start: String?,
    actionCreateTime: String?,
    habitCreateTime: String?,
    freqNum: Int?,
    freqDen: Int?,
    date: LocalDate
): Boolean {
    val numerator = freqNum ?: 1
    val denominator = freqDen ?: 1
    if (numerator <= 0 || denominator <= 0) return false

    val anchorDate = sequenceOf(start, actionCreateTime, habitCreateTime)
        .mapNotNull { value ->
            value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }
        .firstOrNull() ?: LocalDate.of(1970, 1, 1)
    if (date.isBefore(anchorDate)) return false
    if (numerator >= denominator) return true

    val dayInCycle = ChronoUnit.DAYS.between(anchorDate, date) % denominator
    return (0 until numerator).any { occurrence ->
        val dueOffset = (occurrence.toLong() * denominator + numerator / 2L) / numerator
        dayInCycle == dueOffset
    }
}
