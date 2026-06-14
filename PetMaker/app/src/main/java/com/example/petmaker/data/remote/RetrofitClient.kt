package com.example.petmaker.data.remote

import com.example.petmaker.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /** 디버그 빌드에서만 HTTP 바디 전체 로깅, 릴리즈에서는 비활성화 */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /** 공통 기본 클라이언트 (30초 타임아웃) */
    private val baseClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** OpenAI 이미지 생성용 클라이언트 (이미지 생성 시간 고려 90초 타임아웃) */
    private val openAiClient = baseClient.newBuilder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun getWeatherApi(): WeatherApi = buildApi(
        baseUrl = "https://api.openweathermap.org/",
        client = baseClient,
        clazz = WeatherApi::class.java
    )

    fun getGeminiApi(): GeminiApi = buildApi(
        baseUrl = "https://generativelanguage.googleapis.com/",
        client = baseClient,
        clazz = GeminiApi::class.java
    )

    fun getOpenAiApi(): OpenAiApi = buildApi(
        baseUrl = "https://api.openai.com/",
        client = openAiClient,
        clazz = OpenAiApi::class.java
    )

    private fun <T> buildApi(baseUrl: String, client: OkHttpClient, clazz: Class<T>): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(clazz)
}
