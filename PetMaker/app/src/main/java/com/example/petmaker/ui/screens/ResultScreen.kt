package com.example.petmaker.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.local.PetEntity
import com.example.petmaker.data.remote.PetCreationData
import com.example.petmaker.ui.components.StarParticleEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    petData: PetCreationData,
    tempImagePath: String,
    weather: String,
    temperature: Double,
    timezone: String,
    location: String,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showParticles by remember { mutableStateOf(true) }

    // 2초 동안 별 파티클 연출 표시
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showParticles = false
    }

    // 파일을 임시 캐시 디렉토리에서 영구 파일 디렉토리로 이동/복사
    fun saveImagePermanently(tempPath: String): String {
        val tempFile = File(tempPath)
        if (!tempFile.exists()) return tempPath

        val permFile = File(context.filesDir, "pet_image_${System.currentTimeMillis()}.webp")
        try {
            FileInputStream(tempFile).use { input ->
                FileOutputStream(permFile).use { output ->
                    input.copyTo(output)
                }
            }
            return permFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return tempPath
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF120C24)) // 딥퍼플 -> 다크그레이
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. 원형 펫 이미지 프레임
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = tempImagePath,
                    contentDescription = "Generated Pet Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 2. 펫 이름
            Text(
                text = petData.name,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 3. 환경 컨텍스트 요약
            Text(
                text = "$weather • ${temperature.toInt()}°C • $timezone",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            // 4. 상세 묘사 정보 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 외형 및 특징
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "✨ 외형 & 특징",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = petData.description,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // 성격
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "♡ 성격",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = petData.personality,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // 특성 뱃지 리스트
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
                            petData.traits.forEach { trait ->
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

            Spacer(modifier = Modifier.height(16.dp))

            // 5. 하단 액션 버튼
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 도감에 수집하기
                Button(
                    onClick = {
                        scope.launch {
                            val permanentPath = withContext(Dispatchers.IO) {
                                saveImagePermanently(tempImagePath)
                            }
                            val petEntity = PetEntity(
                                petData.name,
                                petData.description,
                                petData.personality,
                                petData.traits,
                                weather,
                                temperature,
                                timezone,
                                location,
                                System.currentTimeMillis(),
                                permanentPath
                            )
                            withContext(Dispatchers.IO) {
                                PetDatabase.getInstance(context).petDao().insertPet(petEntity)
                            }
                            onSaveSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676) // 그린/에메랄드
                    )
                ) {
                    Text(
                        text = "도감에 수집하기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 다음에 만나기
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = "다음에 만나기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 별 폭죽(파티클) 애니메이션 표시
        if (showParticles) {
            StarParticleEffect()
        }
    }
}
