package com.privacypal.app.domain.risk

/** How worrying a granted permission (or a whole app) is. Ordered: compare with `<` / `maxOf`. */
enum class RiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}
