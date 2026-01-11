package com.example.tobe.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
        val TIMEOUT_HOURS = intPreferencesKey("timeout_hours")
        val TRUSTED_CONTACT_NAME = stringPreferencesKey("trusted_contact_name")
        val TRUSTED_CONTACT_PHONE = stringPreferencesKey("trusted_contact_phone")
        val CUSTOM_SMS_MESSAGE = stringPreferencesKey("custom_sms_message")
        val IS_SMS_ENABLED = booleanPreferencesKey("is_sms_enabled")
        val IS_MONITORING_ENABLED = booleanPreferencesKey("is_monitoring_enabled")
        
        // Internal state to avoid duplicate SMS
        val LAST_SMS_SENT_TIMESTAMP = longPreferencesKey("last_sms_sent_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                lastActiveTime = preferences[PreferencesKeys.LAST_ACTIVE_TIMESTAMP] ?: System.currentTimeMillis(),
                timeoutHours = preferences[PreferencesKeys.TIMEOUT_HOURS] ?: 24,
                contactName = preferences[PreferencesKeys.TRUSTED_CONTACT_NAME] ?: "",
                contactPhone = preferences[PreferencesKeys.TRUSTED_CONTACT_PHONE] ?: "",
                smsMessage = preferences[PreferencesKeys.CUSTOM_SMS_MESSAGE] ?: "我已经好久没用手机了，方便的话给我打个电话吧。",
                isSmsEnabled = preferences[PreferencesKeys.IS_SMS_ENABLED] ?: false,
                isMonitoringEnabled = preferences[PreferencesKeys.IS_MONITORING_ENABLED] ?: true,
                lastSmsSentTime = preferences[PreferencesKeys.LAST_SMS_SENT_TIMESTAMP] ?: 0L
            )
        }

    suspend fun updateLastActiveTime(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ACTIVE_TIMESTAMP] = timestamp
            // Reset SMS sent state when active
            preferences[PreferencesKeys.LAST_SMS_SENT_TIMESTAMP] = 0L 
        }
    }

    suspend fun updateTimeout(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMEOUT_HOURS] = hours
        }
    }

    suspend fun updateContact(name: String, phone: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRUSTED_CONTACT_NAME] = name
            preferences[PreferencesKeys.TRUSTED_CONTACT_PHONE] = phone
        }
    }

    suspend fun updateSmsMessage(message: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_SMS_MESSAGE] = message
        }
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SMS_ENABLED] = enabled
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MONITORING_ENABLED] = enabled
        }
    }
    
    suspend fun markSmsSent() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SMS_SENT_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}

data class UserPreferences(
    val lastActiveTime: Long,
    val timeoutHours: Int,
    val contactName: String,
    val contactPhone: String,
    val smsMessage: String,
    val isSmsEnabled: Boolean,
    val isMonitoringEnabled: Boolean,
    val lastSmsSentTime: Long
)
