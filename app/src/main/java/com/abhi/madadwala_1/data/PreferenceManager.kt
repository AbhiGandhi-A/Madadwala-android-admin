package com.abhi.madadwala_1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class NotificationData(
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PreferenceManager(private val context: Context) {
    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_ONLINE_STATUS = booleanPreferencesKey("preferred_online_status")
        val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val UNREAD_NOTIFICATIONS_COUNT = androidx.datastore.preferences.core.intPreferencesKey("unread_notifications_count")
        val SAVED_NOTIFICATIONS = stringPreferencesKey("saved_notifications")
    }

    private val gson = Gson()

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val preferredOnlineStatus: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_ONLINE_STATUS] ?: true
    }

    val isNotificationSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_SOUND_ENABLED] ?: true
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "en"
    }

    val unreadNotificationsCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[UNREAD_NOTIFICATIONS_COUNT] ?: 0
    }

    val savedNotifications: Flow<List<NotificationData>> = context.dataStore.data.map { preferences ->
        val json = preferences[SAVED_NOTIFICATIONS] ?: "[]"
        try {
            val type = object : TypeToken<List<NotificationData>>() {}.type
            gson.fromJson<List<NotificationData>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setPreferredOnlineStatus(online: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_ONLINE_STATUS] = online
        }
    }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setUnreadNotificationsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[UNREAD_NOTIFICATIONS_COUNT] = count
        }
    }

    suspend fun incrementUnreadNotificationsCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[UNREAD_NOTIFICATIONS_COUNT] ?: 0
            preferences[UNREAD_NOTIFICATIONS_COUNT] = current + 1
        }
    }

    suspend fun saveNotification(notification: NotificationData) {
        context.dataStore.edit { preferences ->
            val json = preferences[SAVED_NOTIFICATIONS] ?: "[]"
            try {
                val type = object : TypeToken<MutableList<NotificationData>>() {}.type
                val list: MutableList<NotificationData> = gson.fromJson(json, type) ?: mutableListOf()
                
                // Avoid duplicates (e.g. if saved via service and activity)
                val isDuplicate = list.any { it.title == notification.title && it.message == notification.message && Math.abs(it.timestamp - notification.timestamp) < 5000 }
                
                if (!isDuplicate) {
                    list.add(0, notification) // Add to top
                    // Keep only last 50 notifications
                    val resultList = if (list.size > 50) list.take(50) else list
                    preferences[SAVED_NOTIFICATIONS] = gson.toJson(resultList)
                }
            } catch (e: Exception) {
                val newList = mutableListOf(notification)
                preferences[SAVED_NOTIFICATIONS] = gson.toJson(newList)
            }
        }
    }

    suspend fun clearNotifications() {
        context.dataStore.edit { preferences ->
            preferences[SAVED_NOTIFICATIONS] = "[]"
            preferences[UNREAD_NOTIFICATIONS_COUNT] = 0
        }
    }
}
