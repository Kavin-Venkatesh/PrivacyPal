package com.privacypal.app.data.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.privacypal.app.di.IoDispatcher
import com.privacypal.app.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PackageManagerAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InstalledAppsSource {

    private val packageManager: PackageManager get() = context.packageManager

    override suspend fun scanAll(): List<InstalledApp> = withContext(ioDispatcher) {
        val launchable = launcherPackages()
        installedPackages()
            .filter { it.packageName != context.packageName }
            .map { it.toInstalledApp(labelOf(it), it.packageName in launchable) }
    }

    override suspend fun scan(packageName: String): InstalledApp? = withContext(ioDispatcher) {
        if (packageName == context.packageName) return@withContext null
        val info = try {
            packageInfo(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        info.toInstalledApp(labelOf(info), hasLauncherIcon(packageName))
    }

    private fun installedPackages(): List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

    private fun packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    private fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
        return activities.mapTo(hashSetOf()) { it.activityInfo.packageName }
    }

    private fun hasLauncherIcon(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    private fun labelOf(info: PackageInfo): String =
        info.applicationInfo?.loadLabel(packageManager)?.toString() ?: info.packageName
}
