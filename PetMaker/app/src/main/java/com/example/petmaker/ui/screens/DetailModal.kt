package com.example.petmaker.ui.screens

import android.content.Context
import android.widget.Toast
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.petmaker.data.local.PetConstants
import com.example.petmaker.data.local.PetEntity
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.remote.RetrofitClient
import com.example.petmaker.data.remote.GeminiRequest
import com.example.petmaker.data.remote.GeminiContent
import com.example.petmaker.data.remote.GeminiPart
import com.example.petmaker.data.remote.GeminiInlineData
import com.example.petmaker.data.remote.OpenAiImageRequest
import com.example.petmaker.data.remote.PetCreationData
import com.example.petmaker.ui.components.RotatingSparkles
import com.example.petmaker.ui.components.ThreeDotLoading
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailModal(
    pet: PetEntity,
    melonCandiesCount: Int,
    onMelonCandiesCountChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val petDao = remember { PetDatabase.getInstance(context).petDao() }
    val apiKeyManager = remember { ApiKeyManager(context) }

    var currentPet by remember(pet.id) { mutableStateOf(pet) }
    var isEvolving by remember { mutableStateOf(false) }
    var evolutionError by remember { mutableStateOf<String?>(null) }

    var candyCount by remember(melonCandiesCount) { mutableStateOf(melonCandiesCount) }

    fun updateCandyCount(newCount: Int) {
        candyCount = newCount
        onMelonCandiesCountChanged(newCount)
    }

    val formattedDate = DateFormat.format("yyyy. M. d. a h:mm:ss", currentPet.timestamp).toString()
    var showFullscreenImage by remember { mutableStateOf(false) }

    // LaunchedEffect for Evolution
    LaunchedEffect(isEvolving) {
        if (isEvolving) {
            withContext(Dispatchers.IO) {
                try {
                    if (apiKeyManager.geminiApiKey.isEmpty()) {
                        throw Exception("Gemini API Key가 설정되지 않았습니다. 설정에서 키를 먼저 입력해 주세요.")
                    }
                    if (apiKeyManager.openaiApiKey.isEmpty()) {
                        throw Exception("OpenAI API Key가 설정되지 않았습니다. 설정에서 키를 먼저 입력해 주세요.")
                    }

                    // 1. 기존 등록된 펫 이름 조회
                    val existingNames = petDao.getAllPetNames() ?: emptyList()

                    // 2. Gemini API 호출을 위한 이미지 Base64 인코딩
                    val base64Image = try {
                        val imgFile = File(currentPet.imagePath)
                        if (imgFile.exists()) {
                            val bytes = imgFile.readBytes()
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } else {
                            ""
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }

                    val geminiApi = RetrofitClient.getGeminiApi()
                    val prompt = """
                        당신은 글로벌 탑 티어의 2D 몬스터/판타지 캐릭터 디자이너이자 크리에이티브 디렉터입니다.
                        기존 펫이 성장하여 새로운 모습으로 **진화(Evolution)**하는 단계를 설계하고 디자인해 주세요.
                        
                        기존 펫의 정보:
                        - 이름: ${currentPet.name}
                        - 설명: ${currentPet.description}
                        - 성격: ${currentPet.personality}
                        - 특성: ${currentPet.traits.joinToString(", ")}
                        - 현재 레벨: level ${currentPet.level}
                        - 탄생 위치: ${currentPet.location}
                        - 탄생 당시 날씨: ${currentPet.weather}
                        - 탄생 당시 기온: ${currentPet.temperature}°C
                        
                        진화 디자인 지침:
                        1. 첨부된 기존 펫 이미지를 직접 세심하게 관찰(눈의 형태, 몸의 실루엣 및 색감 조합, 고유 장식 등)한 후, 기존 캐릭터의 핵심 컨셉(환경 속성, 색상 배합, 모티브)을 완벽히 계승하면서도, 더 성숙하고 강력하며 세련된 2D 극장판 애니메이션 스타일의 다음 성장 단계(level ${currentPet.level + 1}) 모습을 디자인해 주세요.
                        2. 포켓몬의 진화 단계(예: 파이리 -> 리자드 -> 리자몽, 이브이 -> 진화형)처럼, 실루엣과 외형의 크기, 디테일이 발전해야 합니다. 눈매도 더 성숙하고 정교한 형태로 진화합니다.
                        3. 그림체 스타일: premium monster collecting RPG creature, high-quality 2D digital anime illustration, crisp clean digital line art, smooth gradient shading, vibrant and vivid colors, evolution-ready anatomy, full body character art, masterpiece quality.
                        4. 배경: 깨끗하고 선명한 흰색 단색 배경(clean solid white background, studio backdrop).
                        5. 이름 규칙 (매우 중요):
                           - **진화된 펫의 이름은 반드시 2~4글자의 순수 한글(공백 없는 오직 한글 자모 결합된 글자)로만 지어주세요.** 영어나 한자, 숫자, 기호, 공백은 절대 포함될 수 없습니다.
                           - 아래 목록에 있는 이름들은 이미 사용 중이므로 **절대 중복되지 않는 새로운 독창적인 이름**이어야 합니다.
                           - 이미 사용 중인 이름 목록: [${existingNames.joinToString(", ")}]
                        
                        반드시 다음 JSON 스키마 형식으로만 응답을 반환하세요. 마크다운 백틱(```json)이나 다른 설명 없이 오직 JSON 텍스트만 출력하세요:
                        {
                            "name": "규칙에 맞는 중복되지 않는 순수 한글 진화형 이름",
                            "description": "level ${currentPet.level + 1}로 진화한 펫의 상세한 성숙기 외형 묘사, 진화된 특징, 환경 요소의 심화 표현을 담은 설명 (한국어, 반드시 2문장 이내로 아주 간결하게)",
                            "english_visual_prompt": "DALL-E 2 / GPT Image 최적화용 고화질 영문 프롬프트 (예: 'An evolved, larger and more majestic version of the winter snow creature in official Pokémon concept art style. High-quality 2D digital anime illustration with crisp clean line art and smooth gradient shading. It has larger glowing ice crystal horns, a mature and confident expression with glossy round blue eyes, and a flowy elegant aura. Clean, solid white background, masterpiece quality.')",
                            "personality": "진화하면서 변화하거나 더 성숙해진 성격 설명 (한국어, 반드시 1문장 이내로 아주 간결하게)",
                            "traits": ["진화형의 특징을 대변하는 3개 이하의 단어형 태그 목록 (한국어)"]
                        }
                    """.trimIndent()

                    val partsList = mutableListOf<GeminiPart>()
                    partsList.add(GeminiPart(text = prompt))
                    
                    if (base64Image.isNotEmpty()) {
                        partsList.add(
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/webp",
                                    data = base64Image
                                )
                            )
                        )
                    }

                    val geminiRequest = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                parts = partsList
                            )
                        )
                    )

                    val modelsToTry = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-flash")
                    var responseText: String? = null
                    var geminiErrorLog = ""

                    for (model in modelsToTry) {
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
                                geminiErrorLog += "[$model Code: ${geminiCall.code()} - $errBody] "
                            }
                        } catch (e: Exception) {
                            geminiErrorLog += "[$model Error: ${e.message}] "
                        }
                        if (responseText != null) break
                    }

                    if (responseText == null) {
                        throw Exception("Gemini API 호출에 모두 실패했습니다. [$geminiErrorLog]")
                    }

                    fun String.stripFence(): String = when {
                        startsWith("```json") -> removePrefix("```json").removeSuffix("```").trim()
                        startsWith("```") -> removePrefix("```").removeSuffix("```").trim()
                        else -> trim()
                    }
                    val cleanJson = responseText.stripFence()

                    var petCreationData = Gson().fromJson(cleanJson, PetCreationData::class.java)

                    var name = petCreationData.name.trim()
                    var description = petCreationData.description
                    var englishVisualPrompt = petCreationData.englishVisualPrompt
                    var personality = petCreationData.personality
                    var traits = petCreationData.traits

                    var retryCount = 0
                    val maxRetries = 3

                    while (retryCount < maxRetries) {
                        val isKorean = name.isNotEmpty() && name.matches(Regex("^[가-힣]+$"))
                        val isDuplicate = existingNames.any { it.equals(name, ignoreCase = true) }
                        if (isKorean && !isDuplicate) {
                            break
                        }

                        retryCount++
                        val feedbackPrompt = """
                            이전 진화 생성 결과에 이름 규칙 위반 또는 중복이 발견되었습니다:
                            - 생성된 진화형 이름: "$name"
                            ${if (isDuplicate) "- 오류: 이 이름은 이미 도감에 등록된 중복 이름입니다. 절대 중복되지 않는 완전히 새로운 독창적인 이름을 지어주세요." else ""}
                            ${if (!isKorean) "- 오류: 이름에 한글 이외의 문자(영어, 숫자, 기호, 공백 등)가 포함되어 있습니다. 공백 없는 오직 순수 한글로만 구성해 주세요." else ""}
                            
                            위 오류를 해결하여 다시 한 번 아래 JSON 스키마 형식으로만 응답해 주세요. 이름은 반드시 중복되지 않는 순수 한글이어야 합니다.
                            마크다운 백틱(```json)이나 다른 설명 없이 오직 JSON 텍스트만 출력하세요:
                            {
                                "name": "중복되지 않는 순수 한글 진화형 이름",
                                "description": "진화형 외형 설명 (한국어)",
                                "english_visual_prompt": "DALL-E 이미지용 고화질 영문 프롬프트",
                                "personality": "성격 설명 (한국어)",
                                "traits": ["특성 태그"]
                            }
                        """.trimIndent()

                        val retryRequest = GeminiRequest(
                            contents = listOf(
                                GeminiContent(
                                    parts = listOf(GeminiPart(text = feedbackPrompt), GeminiPart(inlineData = GeminiInlineData(mimeType = "image/webp", data = base64Image)))
                                )
                            )
                        )

                        var retryResponseText: String? = null
                        for (model in modelsToTry) {
                            try {
                                val call = geminiApi.generateContent(model, apiKeyManager.geminiApiKey, retryRequest).execute()
                                if (call.isSuccessful && call.body() != null) {
                                    val text = call.body()!!.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                                    if (!text.isNullOrEmpty()) {
                                        retryResponseText = text
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (!retryResponseText.isNullOrEmpty()) {
                            val cleanRetryJson = retryResponseText.stripFence()
                            try {
                                val parsed = Gson().fromJson(cleanRetryJson, PetCreationData::class.java)
                                name = parsed.name.trim()
                                description = parsed.description
                                englishVisualPrompt = parsed.englishVisualPrompt
                                personality = parsed.personality
                                traits = parsed.traits
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // 3회 재시도 후에도 실패한 경우 최종 안전장치 — PetConstants 공통 로직 사용
                    val isFinalKorean = name.matches(Regex("^[가-힣]+$"))
                    val isFinalDuplicate = existingNames.any { it.equals(name, ignoreCase = true) }
                    if (!isFinalKorean || isFinalDuplicate) {
                        val sanitized = name.filter { it in '가'..'힣' }.ifEmpty { "포포" }
                        name = PetConstants.generateUniqueName(candidate = sanitized, existingNames = existingNames)
                    }

                    // 3. OpenAI API 호출 및 이미지 디코딩/다운로드
                    val openAiApi = RetrofitClient.getOpenAiApi()
                    val openAiAuth = "Bearer ${apiKeyManager.openAiApiKey}"
                    val openAiRequest = OpenAiImageRequest(
                        model = "gpt-image-2",
                        prompt = englishVisualPrompt,
                        n = 1,
                        size = "1024x1024",
                        quality = "low"
                    )
                    val imageCall = openAiApi.generateImage(openAiAuth, openAiRequest).execute()
                    if (!imageCall.isSuccessful) {
                        val err = imageCall.errorBody()?.string() ?: ""
                        throw Exception("OpenAI 이미지 생성 실패: Code ${imageCall.code()} - $err")
                    }
                    val imageData = imageCall.body()?.data?.firstOrNull()
                        ?: throw Exception("OpenAI 응답에 이미지 데이터가 없습니다.")

                    val imageBytes: ByteArray = when {
                        !imageData.b64Json.isNullOrEmpty() -> {
                            android.util.Base64.decode(imageData.b64Json, android.util.Base64.DEFAULT)
                        }
                        !imageData.url.isNullOrEmpty() -> {
                            val client = OkHttpClient.Builder()
                                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            val dlResponse = client.newCall(Request.Builder().url(imageData.url).build()).execute()
                            if (!dlResponse.isSuccessful) throw Exception("이미지 다운로드 실패 (HTTP ${dlResponse.code})")
                            dlResponse.body?.bytes() ?: throw Exception("이미지 응답 바디가 비어 있습니다.")
                        }
                        else -> throw Exception("반환된 이미지 데이터(b64_json / url)가 모두 비어 있습니다.")
                    }

                    val permFile = File(context.filesDir, "pet_image_${System.currentTimeMillis()}.webp")
                    FileOutputStream(permFile).use { it.write(imageBytes) }

                    // 4. DB 갱신 및 로컬 상태 변경
                    val evolvedPet = PetEntity().apply {
                        setId(currentPet.id)
                        setName(name)
                        setDescription(description)
                        setPersonality(personality)
                        setTraits(traits)
                        setWeather(currentPet.weather)
                        setTemperature(currentPet.temperature)
                        setTimezone(currentPet.timezone)
                        setLocation(currentPet.location)
                        setTimestamp(System.currentTimeMillis())
                        setImagePath(permFile.absolutePath)
                        setLevel(currentPet.level + 1)
                        setAffinity(0)
                        setPartner(currentPet.isPartner)
                        setAccumulatedDistance(currentPet.accumulatedDistance)
                        setAffection(currentPet.affection)
                    }

                    petDao.insertPet(evolvedPet)

                    withContext(Dispatchers.Main) {
                        currentPet = evolvedPet
                        isEvolving = false
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        evolutionError = e.message ?: "진화 중 오류가 발생했습니다."
                        isEvolving = false
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF311B92), Color(0xFF1A0C36)) // 딥퍼플 -> 다크그레이/바이올렛
                    )
                ),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 원형 프로필 이미지
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { showFullscreenImage = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentPet.imagePath,
                            contentDescription = "Pet Detail Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                // 2. 펫 이름
                Text(
                    text = currentPet.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 2.2 파트너 지정 버튼
                Button(
                    onClick = {
                        val newPartnerStatus = !currentPet.isPartner
                        coroutineScope.launch {
                            val updatedPet = PetEntity().apply {
                                id = currentPet.id
                                name = currentPet.name
                                description = currentPet.description
                                personality = currentPet.personality
                                traits = currentPet.traits
                                weather = currentPet.weather
                                temperature = currentPet.temperature
                                timezone = currentPet.timezone
                                location = currentPet.location
                                timestamp = currentPet.timestamp
                                imagePath = currentPet.imagePath
                                level = currentPet.level
                                affinity = currentPet.affinity
                                affection = currentPet.affection
                                accumulatedDistance = currentPet.accumulatedDistance
                                isPartner = newPartnerStatus
                            }
                            withContext(Dispatchers.IO) {
                                if (newPartnerStatus) {
                                    petDao.clearPartners()
                                }
                                petDao.insertPet(updatedPet)
                            }
                            currentPet = updatedPet
                            Toast.makeText(
                                context,
                                if (newPartnerStatus) "'${currentPet.name}'이(가) 파트너로 지정되었습니다!" else "파트너 지정이 해제되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPet.isPartner) Color(0xFFFF4081) else Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (currentPet.isPartner) Color.Transparent else Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentPet.isPartner) "💖 파트너 해제" else "⭐ 파트너 지정",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2.5 멜론 사탕 & 호감도 성장 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "성장 단계: ",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "level ${currentPet.level}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🍈 사탕: ",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${candyCount}개",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { updateCandyCount(candyCount + 10) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE040FB).copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(text = "리필 +10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "호감도 (Affinity)",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${currentPet.affinity} / 100",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            LinearProgressIndicator(
                                progress = { currentPet.affinity / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFE040FB),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }

                        val isMaxLevelAndAffinity = currentPet.level >= 3 && currentPet.affinity >= 100
                        Button(
                            onClick = {
                                if (candyCount > 0 && !isMaxLevelAndAffinity) {
                                    updateCandyCount(candyCount - 1)
                                    val newAffinity = currentPet.affinity + 10
                                    val targetAffinity = if (newAffinity >= 100) 100 else newAffinity
                                    
                                    val updatedPet = PetEntity().apply {
                                        setId(currentPet.id)
                                        setName(currentPet.name)
                                        setDescription(currentPet.description)
                                        setPersonality(currentPet.personality)
                                        setTraits(currentPet.traits)
                                        setWeather(currentPet.weather)
                                        setTemperature(currentPet.temperature)
                                        setTimezone(currentPet.timezone)
                                        setLocation(currentPet.location)
                                        setTimestamp(currentPet.timestamp)
                                        setImagePath(currentPet.imagePath)
                                        setLevel(currentPet.level)
                                        setAffinity(targetAffinity)
                                    }
                                    
                                    currentPet = updatedPet
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            petDao.insertPet(updatedPet)
                                        }
                                        if (targetAffinity >= 100 && currentPet.level < 3) {
                                            isEvolving = true
                                        }
                                    }
                                }
                            },
                            enabled = candyCount > 0 && !isMaxLevelAndAffinity && !isEvolving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = if (isMaxLevelAndAffinity) "최대 성장 상태 완료 ✨" else "🍈 멜론 사탕 주기 (-1개, 호감도 +10)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. 생성 환경 카드 (UI_9.png 참고)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "생성 환경",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "🌡️  ${currentPet.temperature.toInt()}°C",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "📍  ${currentPet.location}",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "☀️  ${currentPet.weather}",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "🗓️  ${currentPet.timezone}",
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 4. 상세설명 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 외형 & 특징
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "외형 & 특징",
                                fontSize = 16.sp,
                                  fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentPet.description,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }

                        // 성격
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "성격",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentPet.personality,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }

                        // 특성
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "특성",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentPet.traits.forEach { trait ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = trait,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. 수집일
                Text(
                    text = "수집일: $formattedDate",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. 액션 버튼 (방생 및 닫기)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = "도감에서 방생",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            text = "닫기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Evolution Overlay
            if (isEvolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        RotatingSparkles(size = 140f)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "진화 진행 중...",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "level ${currentPet.level} -> level ${currentPet.level + 1} 단계로 성장하고 있어요",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ThreeDotLoading()
                    }
                }
            }

            // Error Dialog
            if (evolutionError != null) {
                AlertDialog(
                    onDismissRequest = { evolutionError = null },
                    title = { Text("진화 실패") },
                    text = { Text(evolutionError!!) },
                    confirmButton = {
                        Button(onClick = { evolutionError = null }) {
                            Text("확인")
                        }
                    }
                )
            }
        }
    }

    if (showFullscreenImage) {
        FullscreenImageDialog(
            imagePath = currentPet.imagePath,
            onDismiss = { showFullscreenImage = false }
        )
    }
}
}

@Composable
fun FullscreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = "Fullscreen Pet Image",
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable(enabled = true, onClick = {}),
                    contentScale = ContentScale.Fit
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "화면을 터치하면 도감으로 돌아갑니다",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
