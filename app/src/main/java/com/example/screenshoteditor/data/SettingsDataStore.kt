package com.example.screenshoteditor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    
    companion object {
        private val KEY_IMMEDIATE_CAPTURE = booleanPreferencesKey("immediate_capture")
        private val KEY_DELAY_SECONDS = intPreferencesKey("delay_seconds")
        private val KEY_REMEMBER_ACTION = booleanPreferencesKey("remember_action")
        private val KEY_REMEMBERED_ACTION = stringPreferencesKey("remembered_action")
        private val KEY_AUTO_CLEAR_CLIPBOARD = booleanPreferencesKey("auto_clear_clipboard")
        private val KEY_CLEAR_SECONDS = intPreferencesKey("clear_seconds")
        private val KEY_PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
        private val KEY_NOTIFICATION_CONFIRMATION = booleanPreferencesKey("notification_confirmation")
        private val KEY_DISABLE_ON_LOCK = booleanPreferencesKey("disable_on_lock")
    }
    
    data class Settings(
        val immediateCapture: Boolean = true,
        val delaySeconds: Int = 3,
        val rememberAction: Boolean = false,
        val rememberedAction: String? = null,
        val autoClearClipboard: Boolean = false,
        val clearSeconds: Int = 60,
        val persistentNotification: Boolean = true,
        val notificationConfirmation: Boolean = false,
        val disableOnLock: Boolean = true
    )
    
    val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            immediateCapture = preferences[KEY_IMMEDIATE_CAPTURE] ?: true,
            delaySeconds = preferences[KEY_DELAY_SECONDS] ?: 3,
            rememberAction = preferences[KEY_REMEMBER_ACTION] ?: false,
            rememberedAction = preferences[KEY_REMEMBERED_ACTION],
            autoClearClipboard = preferences[KEY_AUTO_CLEAR_CLIPBOARD] ?: false,
            clearSeconds = preferences[KEY_CLEAR_SECONDS] ?: 60,
            persistentNotification = preferences[KEY_PERSISTENT_NOTIFICATION] ?: true,
            notificationConfirmation = preferences[KEY_NOTIFICATION_CONFIRMATION] ?: false,
            disableOnLock = preferences[KEY_DISABLE_ON_LOCK] ?: true
        )
    }
    
    suspend fun updateImmediateCapture(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IMMEDIATE_CAPTURE] = value
        }
    }
    
    suspend fun updateDelaySeconds(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DELAY_SECONDS] = value
        }
    }
    
    suspend fun updateRememberAction(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMEMBER_ACTION] = value
        }
    }
    
    suspend fun updateRememberedAction(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[KEY_REMEMBERED_ACTION] = value
            } else {
                preferences.remove(KEY_REMEMBERED_ACTION)
            }
        }
    }
    
    suspend fun updateAutoClearClipboard(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_CLEAR_CLIPBOARD] = value
        }
    }
    
    suspend fun updateClearSeconds(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CLEAR_SECONDS] = value
        }
    }
    
    suspend fun updatePersistentNotification(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERSISTENT_NOTIFICATION] = value
        }
    }
    
    suspend fun updateNotificationConfirmation(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_CONFIRMATION] = value
        }
    }
    
    suspend fun updateDisableOnLock(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISABLE_ON_LOCK] = value
        }
    }
    
    suspend fun resetRememberedAction() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_REMEMBERED_ACTION)
        }
    }
}
