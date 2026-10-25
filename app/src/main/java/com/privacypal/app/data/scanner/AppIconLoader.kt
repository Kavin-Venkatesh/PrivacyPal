package com.privacypal.app.data.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.privacypal.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Loads app icons on demand. Icons are never stored in the database. */
interface AppIconLoader {
    /** The icon, or null if the app is gone or has no usable icon. */
    suspend fun load(packageName: String): ImageBitmap?
}

@Singleton
class PackageManagerAppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppIconLoader {

    private val cache = LruCache<String, ImageBitmap>(CACHE_SIZE)
    private val sizePx: Int by lazy {
        (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
    }

    override suspend fun load(packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(ioDispatcher) {
            try {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }?.also { cache.put(packageName, it) }
    }

    private companion object {
        const val CACHE_SIZE = 256
        const val ICON_SIZE_DP = 48
    }
}
