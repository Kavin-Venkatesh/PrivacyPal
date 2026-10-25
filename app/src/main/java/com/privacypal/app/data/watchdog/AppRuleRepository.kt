package com.privacypal.app.data.watchdog

import com.privacypal.app.data.db.AppRuleDao
import com.privacypal.app.data.db.AppRuleEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Apps the user chose not to be alerted about. Their use is still recorded. */
interface AppRuleRepository {
    val mutedPackages: Flow<Set<String>>

    suspend fun setMuted(packageName: String, muted: Boolean)
}

class RoomAppRuleRepository @Inject constructor(private val dao: AppRuleDao) : AppRuleRepository {

    override val mutedPackages: Flow<Set<String>> = dao.observeAll()
        .map { rules -> rules.filter { it.muteAlerts }.mapTo(mutableSetOf()) { it.packageName } }
        .distinctUntilChanged()

    override suspend fun setMuted(packageName: String, muted: Boolean) =
        dao.upsert(AppRuleEntity(packageName = packageName, muteAlerts = muted))
}
