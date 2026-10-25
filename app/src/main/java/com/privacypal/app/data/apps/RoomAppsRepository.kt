package com.privacypal.app.data.apps

import com.privacypal.app.data.db.AppDao
import com.privacypal.app.data.db.AppWithGrants
import com.privacypal.app.data.db.toDomain
import com.privacypal.app.data.db.toEntity
import com.privacypal.app.data.db.toGrantEntities
import com.privacypal.app.data.scanner.InstalledAppsSource
import com.privacypal.app.domain.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface Clock {
    fun now(): Long
}

@Singleton
class RoomAppsRepository @Inject constructor(
    private val dao: AppDao,
    private val source: InstalledAppsSource,
    private val clock: Clock,
) : AppsRepository {

    // Full and single-app refreshes must not interleave their writes.
    private val mutex = Mutex()
    private val state = MutableStateFlow(RefreshState.NOT_STARTED)

    override val apps: Flow<List<InstalledApp>> = dao.observeAll().map { rows -> rows.map(AppWithGrants::toDomain) }

    override val refreshState: StateFlow<RefreshState> = state.asStateFlow()

    override fun app(packageName: String): Flow<InstalledApp?> = dao.observe(packageName).map { it?.toDomain() }

    override suspend fun refresh() = mutex.withLock {
        state.value = RefreshState.RUNNING
        val succeeded = runCatchingNonCancellation {
            val scanned = source.scanAll()
            val now = clock.now()
            dao.replaceAll(scanned.map { it.toEntity(now) }, scanned.flatMap { it.toGrantEntities() })
        }
        state.value = if (succeeded) RefreshState.IDLE else RefreshState.FAILED
    }

    override suspend fun refresh(packageName: String) {
        mutex.withLock {
            // A failed single-app rescan keeps the previous data; the next full refresh corrects it.
            runCatchingNonCancellation {
                val app = source.scan(packageName)
                if (app == null) {
                    dao.deleteApp(packageName)
                } else {
                    dao.replaceApp(app.toEntity(clock.now()), app.toGrantEntities())
                }
            }
        }
    }

    /**
     * Runs [block]; returns false instead of throwing. PackageManager can fail with plain runtime
     * exceptions (e.g. the system server dying), and these refreshes run on the app scope where an
     * uncaught exception would crash the app. Errors are not reported anywhere (no crash reporting).
     */
    @Suppress("TooGenericExceptionCaught")
    private inline fun runCatchingNonCancellation(block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
}
