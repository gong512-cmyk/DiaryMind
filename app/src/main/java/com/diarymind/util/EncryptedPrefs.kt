package com.diarymind.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "diarymind_secure_prefs"
private const val KEY_API_KEY = "deepseek_api_key"
private const val KEY_PRIVACY_CONSENT = "privacy_consent_given"

private fun getMasterKey(context: Context): MasterKey {
    return MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
}

private fun getEncryptedPrefs(context: Context): EncryptedSharedPreferences {
    return EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        getMasterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    ) as EncryptedSharedPreferences
}

fun getApiKey(context: Context): String? {
    return getEncryptedPrefs(context).getString(KEY_API_KEY, null)
}

fun saveApiKey(context: Context, apiKey: String) {
    getEncryptedPrefs(context).edit().putString(KEY_API_KEY, apiKey).apply()
}

fun hasPrivacyConsent(context: Context): Boolean {
    return getEncryptedPrefs(context).getBoolean(KEY_PRIVACY_CONSENT, false)
}

fun setPrivacyConsent(context: Context, consented: Boolean) {
    getEncryptedPrefs(context).edit().putBoolean(KEY_PRIVACY_CONSENT, consented).apply()
}
