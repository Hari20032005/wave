package com.stillwater.app.data

import com.stillwater.app.data.db.MonitoredAppDao
import com.stillwater.app.data.db.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoredAppRepository @Inject constructor(
    private val dao: MonitoredAppDao,
) {
    val monitoredApps: Flow<List<MonitoredAppEntity>> = dao.getAll()

    suspend fun enabledPackages(): Set<String> = dao.getEnabledPackages().toSet()

    suspend fun setMonitored(packageName: String, label: String, monitored: Boolean) {
        if (monitored) {
            dao.upsert(MonitoredAppEntity(packageName = packageName, label = label))
        } else {
            dao.delete(packageName)
        }
    }
}
