package com.example.petmaker.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.data.local.PetConstants
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.local.PetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsDialog(
    apiKeyManager: ApiKeyManager,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTestGenerate: (() -> Unit)? = null
) {
    var weatherKey by remember { mutableStateOf(apiKeyManager.weatherApiKey) }
    var geminiKey by remember { mutableStateOf(apiKeyManager.geminiApiKey) }
    var openAiKey by remember { mutableStateOf(apiKeyManager.openAiApiKey) }
    var mockWeatherVal by remember { mutableStateOf(apiKeyManager.mockWeather) }
    var mockTimezoneVal by remember { mutableStateOf(apiKeyManager.mockTimezone) }

    var showWeather by remember { mutableStateOf(false) }
    var showGemini by remember { mutableStateOf(false) }
    var showOpenAi by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val petDao = remember { PetDatabase.getInstance(context).petDao() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("설정 및 API Key") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "API 키들을 입력해 주세요. 코드에 하드코딩되지 않고 기기에만 안전하게 로컬 저장됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ApiKeyTextField(
                    label = "OpenWeather API Key",
                    value = weatherKey,
                    onValueChange = { weatherKey = it },
                    isVisible = showWeather,
                    onVisibilityToggle = { showWeather = !showWeather }
                )
                ApiKeyTextField(
                    label = "Gemini API Key",
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    isVisible = showGemini,
                    onVisibilityToggle = { showGemini = !showGemini }
                )
                ApiKeyTextField(
                    label = "OpenAI API Key (이미지 생성)",
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    isVisible = showOpenAi,
                    onVisibilityToggle = { showOpenAi = !showOpenAi }
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(2.dp))

                // ── 가상 날씨 설정 ────────────────────────────────────────────
                Text(
                    text = "가상 날씨 설정 (테스트/검증용)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ToggleButtonRow(
                    options = listOf("API", "맑음", "비", "눈", "흐림"),
                    selected = mockWeatherVal,
                    onSelect = { mockWeatherVal = it }
                )

                Spacer(Modifier.height(6.dp))

                // ── 가상 시간대 설정 ──────────────────────────────────────────
                Text(
                    text = "가상 시간대 설정 (테스트/검증용)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ToggleButtonRow(
                    options = listOf("API", "아침", "오후", "저녁", "밤"),
                    selected = mockTimezoneVal,
                    onSelect = { mockTimezoneVal = it }
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(2.dp))

                // ── 가상 테스트 도구 ──────────────────────────────────────────
                Text(
                    text = "가상 테스트 도구 (즉시 반영)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 펫 강제 생성 버튼
                    Button(
                        onClick = {
                            if (onTestGenerate != null) {
                                onTestGenerate()
                            } else {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val existingNames = petDao.getAllPetNames() ?: emptyList()
                                    val name = PetConstants.generateUniqueName(existingNames = existingNames)
                                    val mockPet = PetEntity().apply {
                                        setName(name)
                                        setDescription("가상 테스트를 위해 강제 생성된 귀여운 $name 입니다.")
                                        setPersonality("활발하고 호기심 많은 성격")
                                        setTraits(listOf("테스트", "귀여운", "가상"))
                                        setWeather("맑음")
                                        setTemperature(24.0)
                                        setTimezone("Afternoon (오후)")
                                        setLocation("테스트 연구소")
                                        setTimestamp(System.currentTimeMillis())
                                        setImagePath("")
                                        setLevel(1)
                                        setAffinity(0)
                                    }
                                    petDao.insertPet(mockPet)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "가상 펫 '$name'이(가) 강제 생성되었습니다!", Toast.LENGTH_SHORT).show()
                                        onSave()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("펫 강제 생성", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // 레벨 강제 성장 버튼
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val existingNames = petDao.getAllPetNames() ?: emptyList()
                                val collectedCount = existingNames.size

                                var currentLvl = 1
                                while (5 * (currentLvl + 1) * currentLvl <= collectedCount) currentLvl++
                                val targetLvl = currentLvl + 1
                                val petsToAdd = 5 * targetLvl * (targetLvl - 1) - collectedCount

                                if (petsToAdd > 0) {
                                    val mutableNames = existingNames.toMutableList()
                                    repeat(petsToAdd) { i ->
                                        val name = PetConstants.generateUniqueName(existingNames = mutableNames)
                                        mutableNames.add(name)
                                        petDao.insertPet(PetEntity().apply {
                                            setName(name)
                                            setDescription("레벨업 테스트용 가상 펫 $name 입니다.")
                                            setPersonality("온순함")
                                            setTraits(listOf("테스트", "레벨업"))
                                            setWeather("맑음")
                                            setTemperature(20.0)
                                            setTimezone("Morning (아침)")
                                            setLocation("테스트 월드")
                                            setTimestamp(System.currentTimeMillis() + i)
                                            setImagePath("")
                                            setLevel(1)
                                            setAffinity(0)
                                        })
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "모험가 레벨이 LV.${targetLvl}(으)로 강제 성장되었습니다! (펫 ${petsToAdd}마리 추가 생성됨)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSave()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("레벨 강제 성장", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    apiKeyManager.weatherApiKey = weatherKey
                    apiKeyManager.geminiApiKey = geminiKey
                    apiKeyManager.openAiApiKey = openAiKey
                    apiKeyManager.mockWeather = mockWeatherVal
                    apiKeyManager.mockTimezone = mockTimezoneVal
                    onSave()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

/** 선택/비선택 상태를 토글 가능한 버튼 그룹 컴포저블 */
@Composable
private fun ToggleButtonRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            if (selected == option) {
                Button(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(option, fontSize = 10.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(option, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ApiKeyTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onVisibilityToggle) {
                Text(
                    text = if (isVisible) "숨김" else "표시",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}
