package com.example.petmaker.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.data.remote.*
import com.example.petmaker.ui.components.RotatingSparkles
import com.example.petmaker.ui.components.ThreeDotLoading
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

@Composable
fun GenerationScreen(
    apiKeyManager: ApiKeyManager,
    lat: Double,
    lon: Double,
    onGenerationSuccess: (PetCreationData, String) -> Unit,
    onGenerationFailure: (String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 역지오코딩을 통한 한글 구/동 단위 주소 획득
                val addressName = try {
                    val geocoder = Geocoder(context, Locale.KOREA)
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val admin = addr.subLocality ?: addr.locality ?: ""
                        val subAdmin = addr.thoroughfare ?: addr.subThoroughfare ?: ""
                        if (admin.isNotEmpty() || subAdmin.isNotEmpty()) {
                            "서울 $admin $subAdmin".trim()
                        } else {
                            addr.getAddressLine(0) ?: "알 수 없는 위치"
                        }
                    } else {
                        "서울 종로구"
                    }
                } catch (e: Exception) {
                    "서울 종로구"
                }

                // 2. OpenWeatherMap API 호출
                val weatherApi = RetrofitClient.getWeatherApi()
                val weatherResponse = weatherApi.getCurrentWeather(lat, lon, apiKeyManager.weatherApiKey).execute()
                if (!weatherResponse.isSuccessful || weatherResponse.body() == null) {
                    throw Exception("날씨 정보를 수집할 수 없습니다 (API 에러). API 키를 확인해 주세요.")
                }
                val weatherData = weatherResponse.body()!!
                val weatherMain = weatherData.weather.firstOrNull()?.main ?: "Clear"
                val weatherDesc = weatherData.weather.firstOrNull()?.description ?: "맑음"
                val temp = weatherData.main.temp

                // 시간대 문자열 구성
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val timezone = when (hour) {
                    in 6..11 -> "Morning (아침)"
                    in 12..17 -> "Afternoon (오후)"
                    in 18..21 -> "Evening (저녁)"
                    else -> "Night (밤)"
                }

                // 3. Gemini API 호출을 통한 펫 콘셉트 데이터 생성
                val geminiApi = RetrofitClient.getGeminiApi()
                val prompt = """
                    당신은 나만의 펫 메이커 AI 캐릭터 디자이너입니다.
                    다음 제공되는 실시간 환경 데이터를 분석하여, 이 환경에 어울리는 세상에 하나뿐인 독창적이고 귀여운 판타지 디지털 펫을 창작하세요.
                    
                    [환경 데이터]
                    - 위치: $addressName
                    - 날씨: $weatherDesc ($weatherMain)
                    - 기온: ${temp.toInt()}°C
                    - 시간대: $timezone
                    
                    [요구 사항]
                    반드시 다음 JSON 스키마 형식으로만 응답을 반환하세요. 마크다운 백틱(```json) 없이 순수 JSON 문자열로만 응답하세요:
                    {
                        "name": "펫의 한글 이름 (예: 솔라, 레인, 구름이 등 환경에 맞게 작명)",
                        "description": "펫의 전체적인 외형 묘사 (이미지 생성 프롬프트로 쓸 수 있게 형태, 색상, 고유한 특징 등을 자세히 서술, 한글)",
                        "english_visual_prompt": "Flux 이미지 생성용 영문 프롬프트 (귀엽고 완성도 높은 3D 게임 캐릭터 스타일, 3d cute fantasy character concept art, standalone creature, centered, full body, solid background, detailed texture, $weatherMain themes)",
                        "personality": "성격 묘사 (한글)",
                        "traits": ["성격을 대변하는 3개 이하의 단어형 특징 태그 목록"]
                    }
                """.trimIndent()

                val geminiRequest = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )

                val modelsToTry = listOf("gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-2.5-flash", "gemini-2.0-flash")
                var responseText: String? = null
                var geminiErrorLog = ""

                for (model in modelsToTry) {
                    var attempts = 0
                    while (attempts < 2) {
                        try {
                            val geminiCall = geminiApi.generateContent(model, apiKeyManager.geminiApiKey, geminiRequest).execute()
                            if (geminiCall.isSuccessful && geminiCall.body() != null) {
                                val body = geminiCall.body()!!
                                val text = body.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                                if (!text.isNullOrEmpty()) {
                                    responseText = text
                                    break
                                }
                            } else {
                                val errBody = geminiCall.errorBody()?.string() ?: ""
                                geminiErrorLog += "[$model Try ${attempts + 1} Code: ${geminiCall.code()} - $errBody] "
                            }
                        } catch (e: Exception) {
                            geminiErrorLog += "[$model Try ${attempts + 1} Error: ${e.message}] "
                        }
                        attempts++
                        if (responseText != null) break
                        if (attempts < 2) {
                            delay(1000)
                        }
                    }
                    if (responseText != null) break
                }

                if (responseText == null) {
                    throw Exception("Gemini API 호출에 모두 실패했습니다. [$geminiErrorLog]")
                }

                // 백틱 처리
                val cleanJson = if (responseText.startsWith("```json")) {
                    responseText.replace("```json", "").replace("```", "").trim()
                } else if (responseText.startsWith("```")) {
                    responseText.replace("```", "").trim()
                } else {
                    responseText
                }

                val petCreationData = Gson().fromJson(cleanJson, PetCreationData::class.java)

                // 4. Flux 이미지 생성 (Replicate -> HuggingFace 폴백)
                var imageBytes: ByteArray? = null
                val visualPrompt = petCreationData.englishVisualPrompt
                var exceptionLog = ""

                // 4.1 Replicate 우선 시도 (키가 있는 경우)
                if (apiKeyManager.replicateApiKey.isNotEmpty()) {
                    try {
                        val fluxApi = RetrofitClient.getReplicateApi()
                        val replicateAuth = "Bearer ${apiKeyManager.replicateApiKey}"
                        val repReq = ReplicateRequest(input = ReplicateInput(prompt = visualPrompt))
                        
                        val repCall = fluxApi.startReplicatePrediction(replicateAuth, repReq).execute()
                        if (repCall.isSuccessful && repCall.body() != null) {
                            var prediction = repCall.body()!!
                            var status = prediction.status
                            val getUrl = prediction.urls.get
                            var attempts = 0
                            
                            while (status != "succeeded" && status != "failed" && status != "canceled" && attempts < 12) {
                                delay(1500)
                                val statusCall = fluxApi.checkReplicateStatus(replicateAuth, getUrl).execute()
                                if (statusCall.isSuccessful && statusCall.body() != null) {
                                    prediction = statusCall.body()!!
                                    status = prediction.status
                                }
                                attempts++
                            }
                            
                            if (status == "succeeded" && prediction.output != null) {
                                val imgUrl = prediction.output.firstOrNull()
                                if (imgUrl != null) {
                                    val imgResponse = okhttp3.OkHttpClient().newCall(
                                        okhttp3.Request.Builder().url(imgUrl).build()
                                    ).execute()
                                    if (imgResponse.isSuccessful && imgResponse.body != null) {
                                        imageBytes = imgResponse.body!!.bytes()
                                    }
                                }
                            } else {
                                exceptionLog += "Replicate 예측 실패 혹은 타임아웃 ($status). "
                            }
                        } else {
                            exceptionLog += "Replicate 요청 실패: ${repCall.errorBody()?.string()}. "
                        }
                    } catch (e: Exception) {
                        exceptionLog += "Replicate 오류: ${e.message}. "
                    }
                }

                // 4.2 Replicate 실패 시 HuggingFace 무료 API로 자동 폴백
                if (imageBytes == null) {
                    if (apiKeyManager.hfToken.isNotEmpty()) {
                        try {
                            val hfApi = RetrofitClient.getHuggingFaceApi()
                            val hfAuth = "Bearer ${apiKeyManager.hfToken}"
                            val hfReq = HuggingFaceRequest(inputs = visualPrompt)
                            val hfCall = hfApi.generateHuggingFaceImage(hfAuth, hfReq).execute()
                            
                            if (hfCall.isSuccessful && hfCall.body() != null) {
                                imageBytes = hfCall.body()!!.bytes()
                            } else {
                                throw Exception("Hugging Face API 오류: ${hfCall.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            exceptionLog += "HuggingFace 오류: ${e.message}."
                            throw Exception("이미지 생성 실패. Replicate & HuggingFace 모두 실패했습니다. [$exceptionLog]")
                        }
                    } else {
                        throw Exception("Replicate 이미지 생성에 실패했으며(크레딧 부족 등), 폴백할 HuggingFace Token이 설정되지 않았습니다. [$exceptionLog]")
                    }
                }

                // 5. 이미지를 로컬 캐시 디렉토리에 파일로 보관
                val imageFile = File(context.cacheDir, "temp_pet_${System.currentTimeMillis()}.webp")
                val fos = FileOutputStream(imageFile)
                fos.write(imageBytes)
                fos.flush()
                fos.close()

                withContext(Dispatchers.Main) {
                    onGenerationSuccess(petCreationData, imageFile.absolutePath)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onGenerationFailure(e.message ?: "알 수 없는 펫 생성 오류가 발생했습니다.")
                }
            }
        }
    }

    // UI 레이아웃 (UI_5.png 참고)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF880E4F)) // 어두운 보라 -> 버건디/딥핑크
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 중앙 큰 노란색 Sparkles 회전 애니메이션
            RotatingSparkles(size = 140f)

            Spacer(modifier = Modifier.height(16.dp))

            // 로딩 안내 텍스트
            Text(
                text = "펫 생성 중...",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "현재 환경에 맞는 특별한 펫을 만들고 있어요",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3개 점 로딩 바운스 애니메이션
            ThreeDotLoading()
        }
    }
}
