package net.cofcool.sourcebox.data

import java.time.LocalDate

fun isHabitRequiredOnDate(
    baseDate: LocalDate,
    freqNum: Int?,
    freqDen: Int?,
    date: LocalDate
): Boolean {
    if (date.isBefore(baseDate)) return false

    val interval = freqDen ?: return false
    val numerator = freqNum ?: return false

    if (interval <= 0 || numerator != 1) return false

    return !date.isBefore(baseDate.plusDays(interval.toLong()))
}

fun isHabitRequiredOnDate(
    startDate: LocalDate?,
    freqNum: Int?,
    freqDen: Int?,
    date: LocalDate,
    completedDates: Set<LocalDate>
): Boolean {

    if (startDate == null) return false
    if (date.isBefore(startDate)) return false

    val interval = freqDen ?: return false
    val numerator = freqNum ?: return false

    if (interval <= 0 || numerator != 1) return false

    val lastCompleted = completedDates
        .asSequence()
        .filter { !it.isBefore(startDate) && it.isBefore(date) }
        .maxOrNull()

    val baseDate = lastCompleted ?: startDate

    return !date.isBefore(baseDate.plusDays(interval.toLong()))
}