package com.stillwater.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.work.WorkManager
import com.stillwater.app.data.db.StillwaterDatabase
import com.stillwater.app.data.db.UrgeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M6: the user's data is THEIRS — export it readably, delete it completely.
 * Export writes JSON into cacheDir/exports (shared via FileProvider, never
 * uploaded anywhere by us). Delete is a real wipe: DB + preferences.
 */
@Singleton
class UserDataRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: StillwaterDatabase,
    private val urgeDao: UrgeDao,
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Stillwater")
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        root.put(
            "urgeEvents",
            JSONArray().apply {
                urgeDao.dumpEvents().forEach { e ->
                    put(
                        JSONObject()
                            .put("id", e.id)
                            .put("startedAtEpochMs", e.startedAtEpochMs)
                            .put("endedAtEpochMs", e.endedAtEpochMs)
                            .put("mode", e.mode)
                            .put("entryPoint", e.entryPoint)
                            .put("interceptedPackage", e.interceptedPackage)
                            .put("outcome", e.outcome)
                            .put("furthestStep", e.furthestStep)
                            .put("mood", e.mood)
                            .put("note", e.note),
                    )
                }
            },
        )
        root.put(
            "lapseDebriefs",
            JSONArray().apply {
                urgeDao.dumpDebriefs().forEach { d ->
                    put(
                        JSONObject()
                            .put("urgeEventId", d.urgeEventId)
                            .put("completedAtEpochMs", d.completedAtEpochMs)
                            .put("whatPreceded", d.whatPreceded)
                            .put("nextTimeIdea", d.nextTimeIdea),
                    )
                }
            },
        )
        root.put(
            "eventTriggers",
            JSONArray().apply {
                urgeDao.dumpEventTriggers().forEach { link ->
                    put(JSONObject().put("eventId", link.urgeEventId).put("triggerId", link.triggerId))
                }
            },
        )
        root.put(
            "triggers",
            JSONArray().apply {
                database.triggerDao().getActive().first().forEach { t ->
                    put(JSONObject().put("id", t.id).put("name", t.name).put("category", t.category))
                }
            },
        )
        root.put(
            "plans",
            JSONArray().apply {
                database.planDao().getActivePlans().first().forEach { p ->
                    put(
                        JSONObject()
                            .put("situation", p.situationText)
                            .put("action", p.actionText)
                            .put("rehearsalCount", p.rehearsalCount),
                    )
                }
            },
        )
        root.put(
            "values",
            JSONArray().apply {
                database.userValueDao().getAll().first().forEach { v -> put(v.name) }
            },
        )

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        File(dir, "stillwater-export.json").apply {
            writeText(root.toString(2))
        }
    }

    /** The real wipe behind "delete all my data". */
    suspend fun deleteEverything() {
        WorkManager.getInstance(context).cancelAllWork()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        dataStore.edit { it.clear() }
    }
}
