package com.example.petmaker.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.petmaker.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_keys_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WEATHER = "weather_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
    }

    init {
        // SharedPreferences에 저장된 키가 없거나 비어 있는 경우, BuildConfig에서 제공된 기본값(디폴트)으로 자동 사전 입력
        if (prefs.getString(KEY_WEATHER, "").isNullOrEmpty() && BuildConfig.WEATHER_API_KEY.isNotEmpty()) {
            prefs.edit().putString(KEY_WEATHER, BuildConfig.WEATHER_API_KEY).apply()
        }
        if (prefs.getString(KEY_GEMINI, "").isNullOrEmpty() && BuildConfig.GEMINI_API_KEY.isNotEmpty()) {
            prefs.edit().putString(KEY_GEMINI, BuildConfig.GEMINI_API_KEY).apply()
        }
        if (prefs.getString(KEY_OPENAI, "").isNullOrEmpty() && BuildConfig.OPENAI_API_KEY.isNotEmpty()) {
            prefs.edit().putString(KEY_OPENAI, BuildConfig.OPENAI_API_KEY).apply()
        }
    }

    var weatherApiKey: String
        get() = prefs.getString(KEY_WEATHER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEATHER, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value).apply()

    var openAiApiKey: String
        get() = prefs.getString(KEY_OPENAI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI, value).apply()

    // DetailModal.kt 호환 별칭 (openaiApiKey → openAiApiKey)
    var openaiApiKey: String
        get() = openAiApiKey
        set(value) { openAiApiKey = value }

    var tutorialSeen: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, value).apply()

    var lastClaimedPokedexLevel: Int
        get() = prefs.getInt("last_claimed_pokedex_level", 1)
        set(value) = prefs.edit().putInt("last_claimed_pokedex_level", value).apply()

    var melonCandiesCount: Int
        get() = prefs.getInt("melon_candies_count", 3)
        set(value) = prefs.edit().putInt("melon_candies_count", value).apply()

    var premiumTicketsCount: Int
        get() = prefs.getInt("premium_tickets_count", 0)
        set(value) = prefs.edit().putInt("premium_tickets_count", value).apply()

    var todayWalkedDistance: Float
        get() = prefs.getFloat("today_walked_distance", 0f)
        set(value) = prefs.edit().putFloat("today_walked_distance", value).apply()

    var lastWalkedDate: String
        get() = prefs.getString("last_walked_date", "") ?: ""
        set(value) = prefs.edit().putString("last_walked_date", value).apply()

    var mockWeather: String
        get() = prefs.getString("mock_weather", "API") ?: "API"
        set(value) = prefs.edit().putString("mock_weather", value).apply()

    var mockTimezone: String
        get() = prefs.getString("mock_timezone", "API") ?: "API"
        set(value) = prefs.edit().putString("mock_timezone", value).apply()

    fun hasRequiredKeys(): Boolean {
        // 날씨, Gemini, OpenAI API 키가 모두 채워져야 함
        return weatherApiKey.isNotEmpty() && 
               geminiApiKey.isNotEmpty() &&
               openAiApiKey.isNotEmpty()
    }
}
