package com.mhss.app.otpreader.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.net.Uri
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import com.mhss.app.otpreader.data.datastore.DataStoreRepository
import com.mhss.app.otpreader.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val dataStore: DataStoreRepository,
    private val context: Context
) {

    fun getInstalledApps(): List<InstalledApp> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledPackages(PackageInfoFlags.of(0))
        } else {
            context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        }

        val installedApps = packages.map {
            InstalledApp(
                name = it.applicationInfo.loadLabel(context.packageManager).toString(),
                packageName = it.packageName,
                iconUri = if (it.applicationInfo.icon != 0) Uri.parse("android.resource://" + it.packageName + "/" + it.applicationInfo.icon).toString() else null
            )
        }.sortedBy { it.name }
        return installedApps
    }

    suspend fun <T> savePref(key: Preferences.Key<T>, value: T) {
        dataStore.save(key, value)
    }

    fun <T> getPref(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return dataStore.get(key, defaultValue)
    }

}