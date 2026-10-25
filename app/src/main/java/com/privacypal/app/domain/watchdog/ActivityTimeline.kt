package com.privacypal.app.domain.watchdog

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * What the Activity timeline shows. Empty [sensors] means both.
 *
 * @property packageName only uses whose likely app is this package; null for all.
 */
data class ActivityFilter(
    val sensors: Set<Sensor> = emptySet(),
    val screenOffOnly: Boolean = false,
    val packageName: String? = null,
) {
    val isActive: Boolean get() = sensors.isNotEmpty() || screenOffOnly || packageName != null
}

/** Uses that started on [day], newest first. */
data class DayGroup(val day: LocalDate, val uses: List<SensorUse>)

object ActivityTimeline {

    fun filter(uses: List<SensorUse>, filter: ActivityFilter): List<SensorUse> = uses.filter { use ->
        (filter.sensors.isEmpty() || use.sensor in filter.sensors) &&
            (!filter.screenOffOnly || !use.screenOn) &&
            (filter.packageName == null || use.likelyApp?.packageName == filter.packageName)
    }

    /** Groups [uses] by the local day they started on, newest day and newest use first. */
    fun groupByDay(uses: List<SensorUse>, zone: ZoneId): List<DayGroup> = uses
        .sortedByDescending { it.startedAtMs }
        .groupBy { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate() }
        .map { (day, dayUses) -> DayGroup(day, dayUses) }

    /** The likely apps that appear in [uses], for the app filter; most frequent first. */
    fun likelyPackages(uses: List<SensorUse>): List<String> = uses
        .mapNotNull { it.likelyApp?.packageName }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
}

object HistoryRetention {
    val OPTIONS_DAYS = listOf(7, 30, 90)
    const val DEFAULT_DAYS = 30
    private const val MS_PER_DAY = 24 * 60 * 60 * 1_000L

    /** Uses that started before this time are deleted. */
    fun cutoff(nowMs: Long, days: Int): Long = nowMs - days * MS_PER_DAY
}
