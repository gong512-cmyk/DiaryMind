package com.diarymind.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.diarymind.domain.model.GenerationStyle

private const val PREFS_NAME = "diarymind_secure_prefs"
private const val KEY_API_KEY = "deepseek_api_key"
private const val KEY_PRIVACY_CONSENT = "privacy_consent_given"

private const val KEY_LLM_SUPPLIER = "llm_supplier"
private const val KEY_LLM_BASE_URL = "llm_base_url"
private const val KEY_LLM_MODEL = "llm_model"
private const val KEY_LLM_API_KEY = "llm_api_key"
private const val KEY_LLM_TEMPERATURE = "llm_temperature"
private const val KEY_LLM_MAX_TOKENS = "llm_max_tokens"

data class LlmConfig(
    val supplier: String = "deepseek",
    val baseUrl: String = "https://api.deepseek.com/",
    val model: String = "deepseek-chat",
    val apiKey: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096
)

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

fun getGenerationStyle(context: Context): GenerationStyle {
    val key = getEncryptedPrefs(context).getString(GenerationStyle.PREFS_KEY, null)
    return key?.let { GenerationStyle.fromKey(it) } ?: GenerationStyle.NATURAL
}

fun saveGenerationStyle(context: Context, style: GenerationStyle) {
    getEncryptedPrefs(context).edit().putString(GenerationStyle.PREFS_KEY, style.name).apply()
}

fun getCustomPrompt(context: Context): String? {
    return getEncryptedPrefs(context).getString(GenerationStyle.CUSTOM_PROMPT_KEY, null)
}

fun saveCustomPrompt(context: Context, prompt: String) {
    getEncryptedPrefs(context).edit().putString(GenerationStyle.CUSTOM_PROMPT_KEY, prompt).apply()
}

fun clearCustomPrompt(context: Context) {
    getEncryptedPrefs(context).edit().remove(GenerationStyle.CUSTOM_PROMPT_KEY).apply()
}

fun getLlmConfig(context: Context): LlmConfig {
    val prefs = getEncryptedPrefs(context)
    return LlmConfig(
        supplier = prefs.getString(KEY_LLM_SUPPLIER, "deepseek") ?: "deepseek",
        baseUrl = prefs.getString(KEY_LLM_BASE_URL, "https://api.deepseek.com/") ?: "https://api.deepseek.com/",
        model = prefs.getString(KEY_LLM_MODEL, "deepseek-chat") ?: "deepseek-chat",
        apiKey = prefs.getString(KEY_LLM_API_KEY, null)
            ?: prefs.getString(KEY_API_KEY, "") ?: "",
        temperature = prefs.getFloat(KEY_LLM_TEMPERATURE, 0.7f),
        maxTokens = prefs.getInt(KEY_LLM_MAX_TOKENS, 4096)
    )
}

fun saveLlmConfig(context: Context, config: LlmConfig) {
    getEncryptedPrefs(context).edit().apply {
        putString(KEY_LLM_SUPPLIER, config.supplier)
        putString(KEY_LLM_BASE_URL, config.baseUrl)
        putString(KEY_LLM_MODEL, config.model)
        putString(KEY_LLM_API_KEY, config.apiKey)
        putFloat(KEY_LLM_TEMPERATURE, config.temperature)
        putInt(KEY_LLM_MAX_TOKENS, config.maxTokens)
        apply()
    }
}

fun clearAllApiKeys(context: Context) {
    getEncryptedPrefs(context).edit().apply {
        remove(KEY_API_KEY)
        remove(KEY_LLM_API_KEY)
        remove(KEY_LLM_SUPPLIER)
        remove(KEY_LLM_BASE_URL)
        remove(KEY_LLM_MODEL)
        remove(KEY_LLM_TEMPERATURE)
        remove(KEY_LLM_MAX_TOKENS)
        putBoolean(KEY_PRIVACY_CONSENT, false)
        apply()
    }
}
