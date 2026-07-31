package com.smouldering_durtles.wk.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.smouldering_durtles.wk.Constants.MONTH
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.db.Converters.sessionTypeToString
import com.smouldering_durtles.wk.db.Converters.stringToSessionType
import com.smouldering_durtles.wk.db.model.Property
import com.smouldering_durtles.wk.enums.QuestionType
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.model.AlertContext
import com.smouldering_durtles.wk.util.ObjectSupport
import com.smouldering_durtles.wk.util.ObjectSupport.isEmpty
import com.smouldering_durtles.wk.util.ObjectSupport.isEqualIgnoreCase
import com.smouldering_durtles.wk.util.ObjectSupport.runAsync
import com.smouldering_durtles.wk.util.ObjectSupport.safe
import java.util.concurrent.Semaphore
import java.util.function.Supplier

/**
 * DAO for properties: various key/value records that record useful data that doesn't count as settings.
 */
@Dao
abstract class PropertiesDao {
    private var properties: MutableMap<String, String>? = null

    /**
     * Room-generated method: get all properties.
     *
     * @return the list
     */
    @Query("SELECT * FROM properties ORDER BY name")
    abstract fun getAll(): List<Property>

    /**
     * Preload the property cache.
     */
    fun preload() {
        if (properties != null) {
            return
        }
        val semaphore = Semaphore(0)
        Thread {
            safe {
                val map = HashMap<String, String>()
                getAll().forEach { property -> map[property.name] = property.value }
                properties = map
            }
            semaphore.release()
        }.start()
        safe { semaphore.acquire() }
    }

    private fun getProperty(name: String): String? {
        if (properties == null) {
            preload()
        }
        return properties!![name]
    }

    /**
     * Room-generated method: set a property.
     *
     * @param name the name of the property
     * @param value the value of the property
     */
    @Query("INSERT OR REPLACE INTO properties (name, value) VALUES (:name, :value)")
    protected abstract fun setPropertyHelper(name: String, value: String)

    private fun setProperty(name: String, value: String) {
        if (properties == null) {
            preload()
        }
        properties!![name] = value
        runAsync { setPropertyHelper(name, value) }
    }

    /**
     * Room-generated method: delete a property.
     *
     * @param name the name of the property
     */
    @Query("DELETE FROM properties WHERE name = :name")
    protected abstract fun deletePropertyHelper(name: String)

    /**
     * Delete a property by name, if it exists.
     *
     * @param name the property's name
     */
    fun deleteProperty(name: String) {
        if (properties == null) {
            preload()
        }
        properties!!.remove(name)
        runAsync { deletePropertyHelper(name) }
    }

    private fun getBooleanProperty(name: String): Boolean {
        val value = getProperty(name) ?: return false
        return isEqualIgnoreCase(value, "true")
    }

    private fun setBooleanProperty(name: String, value: Boolean) {
        setProperty(name, value.toString())
    }

    private fun getIntegerProperty(name: String): Int {
        val value = getProperty(name)

        if (isEmpty(value)) {
            return 0
        }

        return safe(0) { value!!.toInt(10) }
    }

    private fun setIntegerProperty(name: String, value: Int) {
        setProperty(name, value.toString())
    }

    private fun getLongProperty(name: String, delta: Long): Long {
        val value = getProperty(name)

        if (isEmpty(value)) {
            return 0
        }

        var longValue = safe(0L) { value!!.toLong(10) }
        if (longValue >= delta) {
            longValue -= delta
        }

        return longValue
    }

    private fun setLongProperty(name: String, value: Long) {
        setProperty(name, value.toString())
    }

    /**
     * Was the API key rejected by WK?.
     *
     * @return true if it was.
     */
    fun isApiKeyRejected(): Boolean = getBooleanProperty("api_key_rejected")

    /**
     * Was the API key rejected by WK?.
     *
     * @param value true if it was.
     */
    fun setApiKeyRejected(value: Boolean) = setBooleanProperty("api_key_rejected", value)

    /**
     * Did the last API call result in an error?.
     *
     * @return true if it did.
     */
    fun isApiInError(): Boolean = getBooleanProperty("api_in_error")

    /**
     * Did the last API call result in an error?.
     *
     * @param value true if it did.
     */
    fun setApiInError(value: Boolean) = setBooleanProperty("api_in_error", value)

    /**
     * When was the last successful API call?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastApiSuccessDate(): Long = getLongProperty("last_api_success", 0)

    /**
     * When was the last successful API call?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastApiSuccessDate(value: Long) = setLongProperty("last_api_success", value)

    /**
     * When was the last successful user sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastUserSyncSuccessDate(): Long = getLongProperty("last_user_sync_success", 0)

    /**
     * When was the last successful user sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastUserSyncSuccessDate(value: Long) = setLongProperty("last_user_sync_success", value)

    /**
     * When was the last successful subject sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    fun getLastSubjectSyncSuccessDate(delta: Long): Long = getLongProperty("last_subject_sync_success", delta)

    /**
     * When was the last successful subject sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastSubjectSyncSuccessDate(value: Long) = setLongProperty("last_subject_sync_success", value)

    /**
     * When was the last successful assignment sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    fun getLastAssignmentSyncSuccessDate(delta: Long): Long = getLongProperty("last_assignment_sync_success", delta)

    /**
     * When was the last successful assignment sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastAssignmentSyncSuccessDate(value: Long) = setLongProperty("last_assignment_sync_success", value)

    /**
     * When was the last successful review statistic sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    fun getLastReviewStatisticSyncSuccessDate(delta: Long): Long =
        getLongProperty("last_review_statistic_sync_success", delta)

    /**
     * When was the last successful review statistic sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastReviewStatisticSyncSuccessDate(value: Long) =
        setLongProperty("last_review_statistic_sync_success", value)

    /**
     * When was the last successful study material sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    fun getLastStudyMaterialSyncSuccessDate(delta: Long): Long =
        getLongProperty("last_study_material_sync_success", delta)

    /**
     * When was the last successful study material sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastStudyMaterialSyncSuccessDate(value: Long) = setLongProperty("last_study_material_sync_success", value)

    /**
     * When was the last successful SRS system sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastSrsSystemSyncSuccessDate(): Long = getLongProperty("last_srs_system_sync_success", 0)

    /**
     * When was the last successful SRS system sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastSrsSystemSyncSuccessDate(value: Long) = setLongProperty("last_srs_system_sync_success", value)

    /**
     * When was the last successful level progression sync?.
     *
     * @param delta a number of ms to subtract from the value
     * @return the timestamp, or 0 if not known
     */
    fun getLastLevelProgressionSyncSuccessDate(delta: Long): Long =
        getLongProperty("last_level_progression_sync_success", delta)

    /**
     * When was the last successful level progression sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastLevelProgressionSyncSuccessDate(value: Long) =
        setLongProperty("last_level_progression_sync_success", value)

    /**
     * When was the last successful summary sync?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastSummarySyncSuccessDate(): Long = getLongProperty("last_summary_sync_success", 0)

    /**
     * When was the last successful summary sync?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastSummarySyncSuccessDate(value: Long) = setLongProperty("last_summary_sync_success", value)

    /**
     * When was the last time there was a check for audio that needed to be downloaded?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastAudioScanDate(): Long = getLongProperty("last_audio_scan", 0)

    /**
     * When was the last time there was a check for audio that needed to be downloaded?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastAudioScanDate(value: Long) = setLongProperty("last_audio_scan", value)

    /**
     * When was the last time there was a check for pitch info that needed to be downloaded?.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastPitchInfoScanDate(): Long = getLongProperty("last_pitch_info_scan", 0)

    /**
     * When was the last time there was a check for pitch info that needed to be downloaded?.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastPitchInfoScanDate(value: Long) = setLongProperty("last_pitch_info_scan", value)

    /**
     * When was the last time a notification was posted? This is the top of the hour of the last update.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastNotificationUpdate(): Long = getLongProperty("last_notification_update", 0)

    /**
     * When was the last time a notification was posted? This is the top of the hour of the last update.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastNotificationUpdate(value: Long) = setLongProperty("last_notification_update", value)

    /**
     * When was the last time a background sync was run? This is the top of the hour of the last update.
     *
     * @return the timestamp, or 0 if not known
     */
    fun getLastBackgroundSync(): Long = getLongProperty("last_background_sync", 0)

    /**
     * When was the last time a background sync was run? This is the top of the hour of the last update.
     *
     * @param value the timestamp, or 0 if not known
     */
    fun setLastBackgroundSync(value: Long) = setLongProperty("last_background_sync", value)

    /**
     * The max level granted by the user's subscription.
     *
     * @return the level, or 3 if not known
     */
    fun getUserMaxLevelGranted(): Int {
        val end = getLongProperty("user_max_level_granted_checked", -MONTH)
        if (end == 0L || System.currentTimeMillis() > end) {
            return 3
        }
        return getIntegerProperty("user_max_level_granted")
    }

    /**
     * The max level granted by the user's subscription.
     *
     * @param value the level, or 3 if not known
     */
    fun setUserMaxLevelGranted(value: Int) {
        setIntegerProperty("user_max_level_granted", value)
        setLongProperty("user_max_level_granted_checked", System.currentTimeMillis())
    }

    /**
     * The user's current level.
     *
     * @return the level, or 0 if not known
     */
    fun getUserLevel(): Int = getIntegerProperty("user_level")

    /**
     * The user's current level.
     *
     * @param value the level, or 0 if not known
     */
    fun setUserLevel(value: Int) = setIntegerProperty("user_level", value)

    /**
     * The user's ID.
     *
     * @return the user ID as a UUID
     */
    fun getUserId(): String? = getProperty("user_id")

    /**
     * The user's ID.
     *
     * @param value the user ID as a UUID
     */
    fun setUserId(value: String) = setProperty("user_id", value)

    /**
     * The user's username.
     *
     * @return the username
     */
    fun getUsername(): String? = getProperty("username")

    /**
     * The user's username.
     *
     * @param value the username
     */
    fun setUsername(value: String) = setProperty("username", value)

    /**
     * Is the user in vacation mode?.
     *
     * @return true if they are
     */
    fun getVacationMode(): Boolean = getBooleanProperty("vacation_mode")

    /**
     * Is the user in vacation mode?.
     *
     * @param value true if they are
     */
    fun setVacationMode(value: Boolean) = setBooleanProperty("vacation_mode", value)

    /**
     * The type of the current session.
     *
     * @return the type
     */
    fun getSessionType(): SessionType = stringToSessionType(getProperty("session_type"))

    /**
     * The type of the current session.
     *
     * @param sessionType the type
     */
    fun setSessionType(sessionType: SessionType) = setProperty("session_type", sessionTypeToString(sessionType))

    /**
     * Is the on/kun setting active for this session?.
     *
     * @return true if it is
     */
    fun getSessionOnkun(): Boolean = getBooleanProperty("session_onkun")

    /**
     * Is the on/kun setting active for this session?.
     *
     * @param sessionOnkun true if it is
     */
    fun setSessionOnkun(sessionOnkun: Boolean) = setBooleanProperty("session_onkun", sessionOnkun)

    /**
     * The version of the currently loaded reference data.
     *
     * @return the version
     */
    fun getReferenceDataVersion(): Int = getIntegerProperty("reference_data_version")

    /**
     * The version of the currently loaded reference data.
     *
     * @param referenceDataVersion the version
     */
    fun setReferenceDataVersion(referenceDataVersion: Int) =
        setIntegerProperty("reference_data_version", referenceDataVersion)

    /**
     * Has a notification been set?.
     *
     * @return true if it has
     */
    fun getNotificationSet(): Boolean = getBooleanProperty("notification_set")

    /**
     * Has a notification been set?.
     *
     * @param notificationSet true if it has
     */
    fun setNotificationSet(notificationSet: Boolean) = setBooleanProperty("notification_set", notificationSet)

    /**
     * Has a forced late refresh of all core models been requested?.
     *
     * This is requested if a subject passes or if the user changes level.
     *
     * @return true if it has
     */
    fun getForceLateRefresh(): Boolean = getBooleanProperty("force_late_refresh")

    /**
     * Has a forced late refresh of all core models been requested?.
     *
     * This is requested if a subject passes or if the user changes level.
     *
     * @param forceLateRefresh  true if it has
     */
    fun setForceLateRefresh(forceLateRefresh: Boolean) = setBooleanProperty("force_late_refresh", forceLateRefresh)

    /**
     * Has a sync reminder been set?.
     *
     * @return true if it has
     */
    fun getSyncReminder(): Boolean = getBooleanProperty("sync_reminder")

    /**
     * Has a sync reminder been set?.
     *
     * @param syncReminder true if it has
     */
    fun setSyncReminder(syncReminder: Boolean) = setBooleanProperty("sync_reminder", syncReminder)

    /**
     * Get the session item ID (subject ID) for the currently shown item in the session.
     *
     * @return the ID
     */
    fun getCurrentItemId(): Long = getLongProperty("current_item_id", 0)

    /**
     * Get the session item ID (subject ID) for the currently shown item in the session.
     *
     * @param currentItemId the ID
     */
    fun setCurrentItemId(currentItemId: Long) = setLongProperty("current_item_id", currentItemId)

    /**
     * Get the question type for the currently shown question in the session.
     *
     * @return the type
     */
    fun getCurrentQuestionType(): QuestionType {
        val value = getProperty("current_question_type") ?: return QuestionType.WANIKANI_RADICAL_NAME
        return try {
            QuestionType.valueOf(value)
        } catch (e: Exception) {
            QuestionType.WANIKANI_RADICAL_NAME
        }
    }

    /**
     * Get the question type for the currently shown question in the session.
     *
     * @param currentQuestionType the type
     */
    fun setCurrentQuestionType(currentQuestionType: QuestionType) =
        setProperty("current_question_type", currentQuestionType.toString())

    /**
     * The last used AlertContext for notifications.
     *
     * @return the context, or a dummy instance if not present
     */
    fun getLastNotificationAlertContext(): AlertContext =
        safe(Supplier { emptyAlertContext() }, ObjectSupport.ThrowingSupplier {
            val s = getProperty("last_notification_alertcontext")
                ?: return@ThrowingSupplier emptyAlertContext()
            Converters.getObjectMapper().readValue(s, AlertContext::class.java)
        })

    /**
     * The last used AlertContext for notifications.
     *
     * @param value the context
     */
    fun setLastNotificationAlertContext(value: AlertContext) {
        safe { setProperty("last_notification_alertcontext", Converters.getObjectMapper().writeValueAsString(value)) }
    }

    /**
     * The last used AlertContext for widgets.
     *
     * @return the context, or a dummy instance if not present
     */
    fun getLastWidgetAlertContext(): AlertContext =
        safe(Supplier { emptyAlertContext() }, ObjectSupport.ThrowingSupplier {
            val s = getProperty("last_widget_alertcontext")
                ?: return@ThrowingSupplier emptyAlertContext()
            Converters.getObjectMapper().readValue(s, AlertContext::class.java)
        })

    /**
     * The last used AlertContext for widgets.
     *
     * @param value the context
     */
    fun setLastWidgetAlertContext(value: AlertContext) {
        safe { setProperty("last_widget_alertcontext", Converters.getObjectMapper().writeValueAsString(value)) }
    }

    /**
     * Has the audio been muted?.
     *
     * @return true if it has
     */
    fun getIsMuted(): Boolean = getBooleanProperty("is_muted")

    /**
     * Has the audio been muted?.
     *
     * @param value true if it has
     */
    fun setIsMuted(value: Boolean) = setBooleanProperty("is_muted", value)

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    fun getMigrationDoneAnkiSplit(): Boolean = getBooleanProperty("migration_done_anki_split")

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    fun setMigrationDoneAnkiSplit(value: Boolean) = setBooleanProperty("migration_done_anki_split", value)

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    fun getMigrationDoneAudio2(): Boolean = getBooleanProperty("migration_done_audio2")

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    fun setMigrationDoneAudio2(value: Boolean) = setBooleanProperty("migration_done_audio2", value)

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    fun getMigrationDoneNotif(): Boolean = getBooleanProperty("migration_done_notif")

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    fun setMigrationDoneNotif(value: Boolean) = setBooleanProperty("migration_done_notif", value)

    /**
     * Has a specific migration been done already?.
     *
     * @return true if it has
     */
    fun getMigrationDoneDump(): Boolean = getBooleanProperty("migration_done_dump")

    /**
     * Set that a specific migration been done already.
     *
     * @param value true if it has
     */
    fun setMigrationDoneDump(value: Boolean) = setBooleanProperty("migration_done_dump", value)

    private companion object {
        /** The dummy AlertContext returned when none has been stored yet, or the stored one is unreadable. */
        fun emptyAlertContext(): AlertContext {
            val ctx = AlertContext()
            ctx.numLessons = -1
            ctx.numReviews = -1
            return ctx
        }
    }
}
