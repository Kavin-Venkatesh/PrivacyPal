package com.privacypal.app.domain.risk

import com.privacypal.app.domain.model.InstalledApp

/**
 * Overall risk of an app: the worst rating among its granted permission groups.
 *
 * @property grants one entry per granted group, worst first.
 * @property flaggedCount granted groups rated MEDIUM or HIGH (worth the user's attention).
 */
data class AppRisk(val level: RiskLevel, val grants: List<GrantRisk>) {
    val flaggedCount: Int
        get() = grants.count { it.level >= RiskLevel.MEDIUM }
}

/** Rates [app] from its currently granted permissions only; requested-but-denied ones don't count. */
fun assessApp(app: InstalledApp): AppRisk {
    val grants = app.grantedGroups
        .map { RiskRules.assess(it, app.category) }
        .sortedWith(compareByDescending<GrantRisk> { it.level }.thenBy { it.group.ordinal })
    return AppRisk(level = grants.maxOfOrNull { it.level } ?: RiskLevel.NONE, grants = grants)
}
