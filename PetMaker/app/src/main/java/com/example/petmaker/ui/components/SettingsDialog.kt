package com.example.petmaker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.petmaker.data.local.ApiKeyManager

@Composable
fun SettingsDialog(
    apiKeyManager: ApiKeyManager,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var weatherKey by remember { mutableStateOf(apiKeyManager.weatherApiKey) }
    var geminiKey by remember { mutableStateOf(apiKeyManager.geminiApiKey) }
    var replicateKey by remember { mutableStateOf(apiKeyManager.replicateApiKey) }
    var hfToken by remember { mutableStateOf(apiKeyManager.hfToken) }

    var showWeather by remember { mutableStateOf(false) }
    var showGemini by remember { mutableStateOf(false) }
    var showReplicate by remember { mutableStateOf(false) }
    var showHf by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "API Key 설정") },
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

                // 1. OpenWeather Map
                ApiKeyTextField(
                    label = "OpenWeather API Key",
                    value = weatherKey,
                    onValueChange = { weatherKey = it },
                    isVisible = showWeather,
                    onVisibilityToggle = { showWeather = !showWeather }
                )

                // 2. Gemini API Key
                ApiKeyTextField(
                    label = "Gemini API Key",
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    isVisible = showGemini,
                    onVisibilityToggle = { showGemini = !showGemini }
                )

                // 3. Replicate API Key
                ApiKeyTextField(
                    label = "Replicate API Key (Flux 유료)",
                    value = replicateKey,
                    onValueChange = { replicateKey = it },
                    isVisible = showReplicate,
                    onVisibilityToggle = { showReplicate = !showReplicate }
                )

                // 4. HuggingFace Token
                ApiKeyTextField(
                    label = "HuggingFace Token (Flux 무료)",
                    value = hfToken,
                    onValueChange = { hfToken = it },
                    isVisible = showHf,
                    onVisibilityToggle = { showHf = !showHf }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    apiKeyManager.weatherApiKey = weatherKey
                    apiKeyManager.geminiApiKey = geminiKey
                    apiKeyManager.replicateApiKey = replicateKey
                    apiKeyManager.hfToken = hfToken
                    onSave()
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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
