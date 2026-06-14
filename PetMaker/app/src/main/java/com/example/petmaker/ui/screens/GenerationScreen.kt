package com.example.petmaker.ui.screens

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
import com.example.petmaker.data.local.PetConstants
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.remote.*
import com.example.petmaker.ui.components.RotatingSparkles
import com.example.petmaker.ui.components.ThreeDotLoading
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// 헬퍼 : Gemini 응답 텍스트에서 마크다운 백틱 제거
// ─────────────────────────────────────────────────────────────────────────────
private fun String.stripMarkdownFence(): String = when {
    startsWith("```json") -> removePrefix("```json").removeSuffix("```").trim()
    startsWith("```") -> removePrefix("```").removeSuffix("```").trim()
    else -> trim()
}

// ─────────────────────────────────────────────────────────────────────────────
// 헬퍼 : 시간대 문자열 반환
// ─────────────────────────────────────────────────────────────────────────────
private fun resolveTimezone(mockTimezone: String): String {
    if (mockTimezone != "API") {
        return when (mockTimezone) {
            "아침" -> "Morning (아침)"
            "오후" -> "Afternoon (오후)"
            "저녁" -> "Evening (저녁)"
            "밤"  -> "Night (밤)"
            else  -> "Morning (아침)"
        }
    }
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..11  -> "Morning (아침)"
        in 12..17 -> "Afternoon (오후)"
        in 18..21 -> "Evening (저녁)"
        else      -> "Night (밤)"
    }
}

@Composable
fun GenerationScreen(
    apiKeyManager: ApiKeyManager,
    weatherDesc: String,
    weatherMain: String,
    temp: Double,
    addressName: String,
    onGenerationSuccess: (PetCreationData, String) -> Unit,
    onGenerationFailure: (String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val petDao = PetDatabase.getInstance(context).petDao()
                val existingNames = petDao.getAllPetNames() ?: emptyList()
                val timezone = resolveTimezone(apiKeyManager.mockTimezone)

                // ── 1. Gemini API로 펫 콘셉트 데이터 생성 ────────────────────
                val geminiApi = RetrofitClient.getGeminiApi()
                val prompt = """
                    당신은 글로벌 탑 티어의 2D 몬스터/판타지 캐릭터 디자이너이자 크리에이티브 디렉터입니다.
                    제공되는 실시간 환경 데이터를 바탕으로, 해당 장소와 날씨, 분위기에 어울리면서도 **포켓몬(Pokémon)과 유사한 형태와 감성을 지닌 세상에 하나뿐인 독창적인 판타지 펫**을 디자인해 주세요.
                    
                    [실시간 환경 데이터]
                    - 위치(장소적 특징 반영): $addressName
                    - 날씨(기상 테마와 원소 반영): $weatherDesc ($weatherMain)
                    - 기온(열기/냉기/온도감 질감 반영): ${temp.toInt()}°C
                    - 시간대(조명 및 감성적 분위기 반영): $timezone
                    
                    [캐릭터 디자인 지침 - 비율: 70% 귀여운 마스코트 / 20% 실시간 환경 적응 외형 / 10% 마법 판타지 원소]
                    1. 포켓몬 스타일 모티브:
                       - 너무 복잡한 디테일을 피하고, 명확하고 단순하면서도 직관적인 몬스터/동물 형태(예: 피카츄, 이브이, 조로아, 님피아 등)를 기본 구조로 잡으세요.
                       - 환경 정보(비, 눈, 맑음, 안개 등)를 캐릭터의 고유 장식이나 신체 부위(예: 눈꽃 목도리, 빗방울 모양 뿔, 안개 날개, 불빛이 나는 꼬리 등) 및 배색에 유기적으로 자연스럽게 녹여내세요.
                    2. 눈(Eye) 디자인 및 묘사:
                       - **펫의 눈 모양은 캐릭터의 전체 컨셉과 완벽히 조화를 이루어야 합니다.**
                       - 귀여운 펫이라면 피카츄나 이브이처럼 크고 맑은 둥근 눈망울(large round sparkling anime eyes, glossy pupils with vivid highlights)을 지녀야 하며,
                       - 날렵하거나 강인한 속성의 펫이라면 한카리아스나 루기아처럼 날카롭고 카리스마 있는 매서운 눈매(sharp fierce eyes, intense pupils, cool determined gaze)를 가져야 합니다.
                       - 생성할 영문 이미지 프롬프트에 눈의 크기, 형태, 하이라이트 상태(예: 'large expressive glossy round anime eyes with white highlights' 또는 'sharp determined eyes with intense cool pupils')를 명확하게 포함해 주세요.
                    3. 그림체 및 화풍 지침 (GPT Image 최적화 - 디지털 애니메이션 스타일):
                       - 그림체 스타일: premium monster collecting RPG creature, high-quality 2D digital anime illustration, crisp clean digital line art, smooth gradient shading, vibrant and vivid colors, rounded silhouette, evolution-ready anatomy, full body character art on clean solid white background, masterpiece quality. (수채화, 색연필, 3D 그래픽, 실사 렌더링은 완전히 배제)
                       - 캐릭터 묘사: 70% cute mascot proportions, 20% local environmental adaptation features, 10% magical fantasy elements. 펫의 포즈와 함께, 몸의 질감, 고유한 특징, 그리고 컨셉에 어울리는 눈 모양(Eye shape)을 명확하게 묘사하세요.
                       - 배경: 캐릭터가 선명하게 돋보이는 깔끔한 흰색 단색 배경(clean solid white background, studio backdrop).
                    4. 이름 규칙 (매우 중요):
                       - **펫의 이름은 반드시 2~4글자의 순수 한글(공백 없는 오직 한글 자모 결합된 글자)로만 지어주세요.** 영어나 한자, 숫자, 기호, 공백은 절대 포함될 수 없습니다.
                       - 아래 목록에 있는 이름들은 이미 사용 중이므로 **절대 중복되지 않는 새로운 독창적인 이름**이어야 합니다.
                       - 이미 사용 중인 이름 목록: [${existingNames.joinToString(", ")}]
                    
                    반드시 다음 JSON 스키마 형식으로만 응답을 반환하세요. 마크다운 백틱(```json)이나 다른 설명 없이 오직 JSON 텍스트만 출력하세요:
                    {
                        "name": "규칙에 맞는 중복되지 않는 순수 한글 펫 이름",
                        "description": "펫의 세부 외형, 묘사된 눈 모양, 특징, 색상 배합 및 환경 요소를 포함한 한국어 설명 (반드시 2문장 이내로 아주 간결하게)",
                        "english_visual_prompt": "고화질 영문 프롬프트",
                        "personality": "펫의 친근하고 환경에 영향받는 성격 묘사 (반드시 1문장 이내로 아주 간결하게)",
                        "traits": ["성격과 특징을 대변하는 3개 이하의 단어형 태그 목록"]
                    }
                """.trimIndent()

                val geminiRequest = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                )
                val modelsToTry = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash")
                var responseText: String? = null
                val geminiErrorLog = StringBuilder()

                for (model in modelsToTry) {
                    try {
                        val call = geminiApi.generateContent(model, apiKeyManager.geminiApiKey, geminiRequest).execute()
                        if (call.isSuccessful) {
                            val text = call.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                            if (!text.isNullOrEmpty()) { responseText = text; break }
                        } else {
                            geminiErrorLog.append("[$model Code:${call.code()} ${call.errorBody()?.string()}] ")
                        }
                    } catch (e: Exception) {
                        geminiErrorLog.append("[$model ${e.message}] ")
                    }
                }
                responseText ?: throw Exception("Gemini API 호출에 모두 실패했습니다. [$geminiErrorLog]")

                var petCreationData = Gson().fromJson(responseText.stripMarkdownFence(), PetCreationData::class.java)
                var name = petCreationData.name.trim()
                var description = petCreationData.description
                var englishVisualPrompt = petCreationData.englishVisualPrompt
                var personality = petCreationData.personality
                var traits = petCreationData.traits

                // ── 2. 이름 규칙 위반 시 Gemini 재시도 (최대 3회) ────────────
                repeat(3) {
                    val isKorean = name.matches(Regex("^[가-힣]+$"))
                    val isDuplicate = existingNames.any { it.equals(name, ignoreCase = true) }
                    if (isKorean && !isDuplicate) return@repeat

                    val feedbackPrompt = """
                        이전 생성 결과에 이름 규칙 위반 또는 중복이 발견되었습니다:
                        - 생성된 이름: "$name"
                        ${if (isDuplicate) "- 오류: 이미 도감에 등록된 중복 이름입니다. 절대 중복되지 않는 새로운 독창적인 이름을 지어주세요." else ""}
                        ${if (!isKorean) "- 오류: 이름에 한글 이외의 문자가 포함되어 있습니다. 순수 한글로만 구성해 주세요." else ""}
                        
                        위 오류를 해결하여 아래 JSON 스키마 형식으로만 응답해 주세요:
                        {"name":"순수 한글 이름","description":"외형 설명","english_visual_prompt":"영문 프롬프트","personality":"성격","traits":["태그"]}
                    """.trimIndent()

                    val retryReq = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = feedbackPrompt))))
                    )
                    for (model in modelsToTry) {
                        try {
                            val call = geminiApi.generateContent(model, apiKeyManager.geminiApiKey, retryReq).execute()
                            if (call.isSuccessful) {
                                val text = call.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                                if (!text.isNullOrEmpty()) {
                                    val parsed = runCatching {
                                        Gson().fromJson(text.stripMarkdownFence(), PetCreationData::class.java)
                                    }.getOrNull()
                                    if (parsed != null) {
                                        name = parsed.name.trim()
                                        description = parsed.description
                                        englishVisualPrompt = parsed.englishVisualPrompt
                                        personality = parsed.personality
                                        traits = parsed.traits
                                    }
                                    break
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                // ── 3. 최종 이름 안전장치 (폴백 이름 사용) ──────────────────
                val isFinalKorean = name.matches(Regex("^[가-힣]+$"))
                val isFinalDuplicate = existingNames.any { it.equals(name, ignoreCase = true) }
                if (!isFinalKorean || isFinalDuplicate) {
                    val sanitized = name.filter { it in '가'..'힣' }.ifEmpty { "포포" }
                    name = PetConstants.generateUniqueName(candidate = sanitized, existingNames = existingNames)
                }

                petCreationData = PetCreationData(name, description, englishVisualPrompt, personality, traits)

                // ── 4. OpenAI 이미지 생성 ─────────────────────────────────────
                val openAiApi = RetrofitClient.getOpenAiApi()
                val openAiRequest = OpenAiImageRequest(
                    model = "gpt-image-2",
                    prompt = englishVisualPrompt,
                    n = 1,
                    size = "1024x1024",
                    quality = "low"
                )
                val authHeader = "Bearer ${apiKeyManager.openAiApiKey}"
                val openAiCall = openAiApi.generateImage(authHeader, openAiRequest).execute()

                if (!openAiCall.isSuccessful) {
                    val errBody = openAiCall.errorBody()?.string() ?: ""
                    throw Exception("OpenAI API 오류 [Code: ${openAiCall.code()} - $errBody]")
                }

                val imageData = openAiCall.body()?.data?.firstOrNull()
                    ?: throw Exception("OpenAI 응답에 이미지 데이터가 없습니다.")

                val imageBytes: ByteArray = when {
                    !imageData.b64Json.isNullOrEmpty() -> {
                        android.util.Base64.decode(imageData.b64Json, android.util.Base64.DEFAULT)
                    }
                    !imageData.url.isNullOrEmpty() -> {
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        val response = client.newCall(okhttp3.Request.Builder().url(imageData.url).build()).execute()
                        if (!response.isSuccessful) throw Exception("이미지 다운로드 실패 (HTTP ${response.code})")
                        response.body?.bytes() ?: throw Exception("이미지 응답 바디가 비어 있습니다.")
                    }
                    else -> throw Exception("반환된 이미지 데이터(b64_json / url)가 모두 비어 있습니다.")
                }

                // ── 5. 이미지를 캐시 파일로 저장 ─────────────────────────────
                val imageFile = File(context.cacheDir, "temp_pet_${System.currentTimeMillis()}.webp")
                FileOutputStream(imageFile).use { it.write(imageBytes) }

                withContext(Dispatchers.Main) {
                    onGenerationSuccess(petCreationData, imageFile.absolutePath)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onGenerationFailure(e.message ?: "알 수 없는 펫 생성 오류가 발생했습니다.")
                }
            }
        }
    }

    // ── UI : 로딩 화면 ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFF311B92), Color(0xFF880E4F)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RotatingSparkles(size = 140f)
            Spacer(modifier = Modifier.height(16.dp))
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
            ThreeDotLoading()
        }
    }
}
