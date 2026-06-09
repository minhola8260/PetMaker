package com.example.petmaker.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

// Replicate Models
data class ReplicateRequest(
    @SerializedName("input") val input: ReplicateInput
)

data class ReplicateInput(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("aspect_ratio") val aspect_ratio: String = "1:1",
    @SerializedName("output_format") val output_format: String = "webp"
)

data class ReplicateResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String, // starting, processing, succeeded, failed, canceled
    @SerializedName("urls") val urls: ReplicateUrls,
    @SerializedName("output") val output: List<String>? // 이미지 URL 리스트
)

data class ReplicateUrls(
    @SerializedName("get") val get: String
)

// HuggingFace Request Model
data class HuggingFaceRequest(
    @SerializedName("inputs") val inputs: String
)

interface FluxApi {
    // 1. Replicate 예측 시작 요청
    @POST("v1/models/black-forest-labs/flux-schnell/predictions")
    fun startReplicatePrediction(
        @Header("Authorization") auth: String, // "Bearer <KEY>"
        @Body request: ReplicateRequest
    ): Call<ReplicateResponse>

    // 2. Replicate 상태 폴링
    @GET
    fun checkReplicateStatus(
        @Header("Authorization") auth: String,
        @Url url: String // urls.get 으로부터 받은 전체 주소
    ): Call<ReplicateResponse>

    // 3. Hugging Face 동기 이미지 생성
    @POST("models/black-forest-labs/FLUX.1-schnell")
    fun generateHuggingFaceImage(
        @Header("Authorization") auth: String, // "Bearer <TOKEN>"
        @Body request: HuggingFaceRequest
    ): Call<ResponseBody>
}
