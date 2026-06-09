package com.example.petmaker.data.local

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_keys_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WEATHER = "weather_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_REPLICATE = "replicate_api_key"
        private const val KEY_HF = "hf_token"
    }

    var weatherApiKey: String
        get() = prefs.getString(KEY_WEATHER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEATHER, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value).apply()

    var replicateApiKey: String
        get() = prefs.getString(KEY_REPLICATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REPLICATE, value).apply()

    var hfToken: String
        get() = prefs.getString(KEY_HF, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HF, value).apply()

    fun hasRequiredKeys(): Boolean {
        // 최소한 날씨 및 Gemini와 함께 이미지 생성을 위한 Replicate 혹은 HuggingFace 키 중 하나가 필요함
        return weatherApiKey.isNotEmpty() && 
               geminiApiKey.isNotEmpty() && 
               (replicateApiKey.isNotEmpty() || hfToken.isNotEmpty())
    }
}
