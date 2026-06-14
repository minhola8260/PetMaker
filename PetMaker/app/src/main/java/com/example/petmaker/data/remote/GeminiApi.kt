package com.example.petmaker.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

import retrofit2.http.Path

// Gemini Request Models
data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>
)

data class GeminiContent(
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("inlineData") val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("data") val data: String
)

// Gemini Response Models
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContentResponse?
)

data class GeminiContentResponse(
    @SerializedName("parts") val parts: List<GeminiPartResponse>?
)

data class GeminiPartResponse(
    @SerializedName("text") val text: String?
)

// 최종 파싱할 펫 데이터 스펙
data class PetCreationData(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("english_visual_prompt") val englishVisualPrompt: String,
    @SerializedName("personality") val personality: String,
    @SerializedName("traits") val traits: List<String>
)

interface GeminiApi {
    @POST("v1/models/{model}:generateContent")
    fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}
