package com.example.petmaker.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class OpenAiImageRequest(
    @SerializedName("model") val model: String = "gpt-image-2",
    @SerializedName("prompt") val prompt: String,
    @SerializedName("n") val n: Int = 1,
    @SerializedName("size") val size: String = "1024x1024",
    @SerializedName("quality") val quality: String = "low"  // low/medium/high - low가 가장 빠름
)

data class OpenAiImageResponse(
    @SerializedName("created") val created: Long,
    @SerializedName("data") val data: List<OpenAiImageData>
)

data class OpenAiImageData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("b64_json") val b64Json: String? = null
)

interface OpenAiApi {
    @POST("v1/images/generations")
    fun generateImage(
        @Header("Authorization") auth: String, // "Bearer <OPENAI_API_KEY>"
        @Body request: OpenAiImageRequest
    ): Call<OpenAiImageResponse>
}
