package com.example.cse227.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.cse227.data.model.ActivePatient

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedOrStandardPrefs(context)

    companion object {
        private const val PREF_NAME = "ap20_secure_session"
        private const val KEY_AUTH_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_TABLET_ID = "registered_tablet_id"
        private const val KEY_PATIENT_ID = "active_patient_id"
        private const val KEY_PATIENT_NAME = "active_patient_name"
        private const val KEY_PATIENT_AGE = "active_patient_age"
        private const val KEY_PATIENT_SEX = "active_patient_sex"
        private const val KEY_PATIENT_DATE = "active_patient_date"
        private const val KEY_BASE_URL = "backend_base_url"

        const val DEFAULT_BASE_URL = "https://healthbackend-ys2q.onrender.com/api/"
    }

    private fun createEncryptedOrStandardPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveAuthSession(token: String, userId: String, username: String, role: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    fun saveTabletId(tabletId: String) {
        prefs.edit().putString(KEY_TABLET_ID, tabletId).apply()
    }

    fun getTabletId(): String? = prefs.getString(KEY_TABLET_ID, null)

    fun saveActivePatientInfo(
        patientId: String?,
        patientName: String?,
        age: Int? = null,
        sex: String? = null,
        date: String? = null
    ) {
        prefs.edit()
            .putString(KEY_PATIENT_ID, patientId)
            .putString(KEY_PATIENT_NAME, patientName)
            .putInt(KEY_PATIENT_AGE, age ?: -1)
            .putString(KEY_PATIENT_SEX, sex)
            .putString(KEY_PATIENT_DATE, date)
            .apply()
    }

    fun getActivePatientId(): String? = prefs.getString(KEY_PATIENT_ID, null)
    fun getActivePatientName(): String? = prefs.getString(KEY_PATIENT_NAME, null)

    fun getCachedActivePatient(): ActivePatient? {
        val pid = getActivePatientId() ?: return null
        val name = getActivePatientName()
        val ageVal = prefs.getInt(KEY_PATIENT_AGE, -1)
        val age = if (ageVal > 0) ageVal else null
        val sex = prefs.getString(KEY_PATIENT_SEX, null)
        val date = prefs.getString(KEY_PATIENT_DATE, null)
        return ActivePatient(
            patientId = pid,
            mongoId = pid,
            patientName = name,
            age = age,
            sex = sex,
            assignmentDate = date,
            date = date,
            isActive = true
        )
    }

    fun saveBaseUrl(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, sanitized).apply()
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        return !token.isNullOrEmpty()
    }

    fun isTabletUser(): Boolean {
        val role = getUserRole()
        return role.equals("tablet", ignoreCase = true)
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    fun clearSession() {
        clearAllData()
    }
}
