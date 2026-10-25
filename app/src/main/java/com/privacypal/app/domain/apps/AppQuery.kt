package com.privacypal.app.domain.apps

import com.privacypal.app.domain.model.InstalledApp
import com.privacypal.app.domain.model.PermissionGroup
import com.privacypal.app.domain.risk.AppRisk
import com.privacypal.app.domain.risk.RiskLevel
import com.privacypal.app.domain.risk.assessApp

/** An app together with its risk assessment. */
data class RatedApp(val app: InstalledApp, val risk: AppRisk)

fun InstalledApp.rated(): RatedApp = RatedApp(this, assessApp(this))

enum class AppFilter {
    ALL,
    HIGH_RISK,
    CAMERA,
    MICROPHONE,
    LOCATION,
    CONTACTS,
}

enum class AppSort {
    /** Highest risk first, then most flagged permissions, then name. */
    RISK,
    NAME,
    RECENTLY_INSTALLED,
}

data class AppQuery(
    val search: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val sort: AppSort = AppSort.RISK,
    val showSystem: Boolean = false,
) {
    /** True when search or filter narrow the list (sort and system toggle don't hide user apps). */
    val isNarrowed: Boolean
        get() = search.isNotBlank() || filter != AppFilter.ALL
}

/** Counts over the apps the user can currently see (system toggle applied, search and filter not). */
data class AppsSummary(
    val total: Int,
    val highRisk: Int,
    val camera: Int,
    val microphone: Int,
    val location: Int,
    val contacts: Int,
) {
    fun count(filter: AppFilter): Int = when (filter) {
        AppFilter.ALL -> total
        AppFilter.HIGH_RISK -> highRisk
        AppFilter.CAMERA -> camera
        AppFilter.MICROPHONE -> microphone
        AppFilter.LOCATION -> location
        AppFilter.CONTACTS -> contacts
    }
}

fun summarize(apps: List<RatedApp>, showSystem: Boolean): AppsSummary {
    val visible = apps.filter { showSystem || !it.app.isSystem }
    fun countOf(filter: AppFilter) = visible.count { it.matches(filter) }
    return AppsSummary(
        total = visible.size,
        highRisk = countOf(AppFilter.HIGH_RISK),
        camera = countOf(AppFilter.CAMERA),
        microphone = countOf(AppFilter.MICROPHONE),
        location = countOf(AppFilter.LOCATION),
        contacts = countOf(AppFilter.CONTACTS),
    )
}

/** Applies the system toggle, search (label or package, case-insensitive), filter and sort. */
fun List<RatedApp>.applyQuery(query: AppQuery): List<RatedApp> {
    val search = query.search.trim()
    return filter { query.showSystem || !it.app.isSystem }
        .filter {
            search.isEmpty() ||
                it.app.label.contains(search, ignoreCase = true) ||
                it.app.packageName.contains(search, ignoreCase = true)
        }
        .filter { it.matches(query.filter) }
        .sortedWith(comparatorFor(query.sort))
}

private fun RatedApp.matches(filter: AppFilter): Boolean {
    val groups = app.grantedGroups
    return when (filter) {
        AppFilter.ALL -> true
        AppFilter.HIGH_RISK -> risk.level == RiskLevel.HIGH
        AppFilter.CAMERA -> PermissionGroup.CAMERA in groups
        AppFilter.MICROPHONE -> PermissionGroup.MICROPHONE in groups
        AppFilter.LOCATION -> PermissionGroup.LOCATION in groups || PermissionGroup.BACKGROUND_LOCATION in groups
        AppFilter.CONTACTS -> PermissionGroup.CONTACTS in groups
    }
}

private val byName: Comparator<RatedApp> =
    compareBy<RatedApp, String>(String.CASE_INSENSITIVE_ORDER) { it.app.label }.thenBy { it.app.packageName }

private fun comparatorFor(sort: AppSort): Comparator<RatedApp> = when (sort) {
    AppSort.RISK -> compareByDescending<RatedApp> { it.risk.level }
        .thenByDescending { it.risk.flaggedCount }
        .then(byName)

    AppSort.NAME -> byName

    AppSort.RECENTLY_INSTALLED -> compareByDescending<RatedApp> { it.app.firstInstallTime }.then(byName)
}
