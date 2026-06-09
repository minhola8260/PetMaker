package com.example.petmaker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.ui.components.SettingsDialog
import com.example.petmaker.ui.components.WeatherBackground
import com.example.petmaker.ui.components.WeatherType
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    apiKeyManager: ApiKeyManager,
    currentWeather: String, // 맑음, 비, 눈, 흐림
    currentTemp: Double,
    currentLocation: String, // 서울 종로구 등
    feelsLikeTemp: Double,
    minTemp: Double,
    maxTemp: Double,
    humidity: Int,
    pressure: Int,
    windSpeed: Double,
    latitude: Double,
    longitude: Double,
    onNavigateToCollection: () -> Unit,
    onGeneratePet: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showWeatherDetails by remember { mutableStateOf(false) }
    var isPetFound by remember { mutableStateOf(false) }
    var currentTimeText by remember { mutableStateOf("") }

    // 1. 실시간 초 단위 시계 업데이트
    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            val ampm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "오전" else "오후"
            val formattedHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            currentTimeText = String.format("%s %d:%02d:%02d", ampm, formattedHour, minute, second)
            delay(1000)
        }
    }

    // 2. 10초마다 펫 출현 확률 체크 (50% 확률)
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            isPetFound = Random.nextFloat() < 0.5f
        }
    }

    // 날씨 한글에 따른 매핑
    val weatherType = remember(currentWeather) {
        when {
            currentWeather.contains("비") -> WeatherType.RAIN
            currentWeather.contains("눈") -> WeatherType.SNOW
            currentWeather.contains("흐림") || currentWeather.contains("구름") || currentWeather.contains("안개") -> WeatherType.CLOUDS
            else -> WeatherType.CLEAR
        }
    }

    // 시간대에 따른 텍스트 매핑 (현재 시각 기준)
    val timeOfDayText = remember(currentTimeText) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 6..11 -> "☀️ Morning"
            in 12..17 -> "☀️ Afternoon"
            in 18..21 -> "🌙 Evening"
            else -> "🌙 Night"
        }
    }

    // 펫 발견 버튼용 무한 스케일(펄스) 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "PulseEffect")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    WeatherBackground(weatherType = weatherType) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (showWeatherDetails) 16.dp else 0.dp),
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Pet Maker",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "환경 기반 AI 펫 생성기",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                // 중앙 메인 카드 UI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 날씨 정보 박스 (반투명 글래스모피즘 스타일 - 클릭 시 상세 다이얼로그 표시)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clickable { showWeatherDetails = true },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.18f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 날씨 및 시간대
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = timeOfDayText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }

                            // 기온
                            Text(
                                text = "${currentTemp.toInt()}°C",
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // 날씨 상태 상세
                            Text(
                                text = currentWeather,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // 위치
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = currentLocation.ifEmpty { "위치 확인 중..." },
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // 현재 시간
                            Text(
                                text = currentTimeText,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👆 카드를 탭하여 상세 환경 정보 보기",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // 하단 인터랙션 영역 (버튼 배치)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. 새로운 펫 발견 버튼
                    AnimatedVisibility(
                        visible = isPetFound,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        Button(
                            onClick = {
                                onGeneratePet()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(pulseScale),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE040FB) // 보라/핑크
                            )
                        ) {
                            Text(
                                text = "✨ 새로운 펫 발견!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 2. 내 펫 도감 보기 버튼
                    Button(
                        onClick = onNavigateToCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.22f)
                        )
                    ) {
                        Text(
                            text = "내 펫 도감 보기",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } // Scaffold 끝

            // 3. 상세 환경 정보 오버레이 (뒷배경 블라인드 및 애니메이션 적용)
            val transitionState = remember { MutableTransitionState(false) }
            LaunchedEffect(showWeatherDetails) {
                transitionState.targetState = showWeatherDetails
            }

            if (transitionState.currentState || transitionState.targetState) {
                val transition = updateTransition(transitionState, label = "OverlayTransition")
                
                val backdropAlpha by transition.animateFloat(
                    transitionSpec = { tween(300) },
                    label = "BackdropAlpha"
                ) { state -> if (state) 0.75f else 0.0f }
                
                val cardScale by transition.animateFloat(
                    transitionSpec = {
                        if (targetState) {
                            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        } else {
                            tween(200)
                        }
                    },
                    label = "CardScale"
                ) { state -> if (state) 1.0f else 0.85f }

                val cardAlpha by transition.animateFloat(
                    transitionSpec = { tween(250) },
                    label = "CardAlpha"
                ) { state -> if (state) 1.0f else 0.0f }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // 뒷배경 블라인드 디밍 레이어 (탭할 경우 닫힘)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = backdropAlpha))
                            .clickable(enabled = showWeatherDetails) { showWeatherDetails = false }
                    )

                    // 상세 환경 대시보드 카드
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .wrapContentHeight()
                            .scale(cardScale)
                            .graphicsLayer { alpha = cardAlpha }
                            .clickable(enabled = true, onClick = {}),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF130924).copy(alpha = 0.96f) // 깊고 그윽한 다크 보라
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE040FB).copy(alpha = 0.6f),
                                    Color(0xFF00B0FF).copy(alpha = 0.6f)
                                )
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 헤더 영역
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "🔍 실시간 환경 감지",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "대기 센서 및 기상 API 지오코딩 동기화 정보",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                
                                // ✕ 닫기 버튼
                                IconButton(
                                    onClick = { showWeatherDetails = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                ) {
                                    Text(
                                        text = "✕",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // 통합 요약 대시보드
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFE040FB).copy(alpha = 0.12f),
                                                Color(0xFF00B0FF).copy(alpha = 0.12f)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = "📍", fontSize = 16.sp)
                                            Text(
                                                text = currentLocation.ifEmpty { "위치 확인 중..." },
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$currentWeather | $currentTimeText",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Text(
                                        text = "${currentTemp.toInt()}°C",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            // 2x4 정밀 그리드
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "🌐",
                                        label = "실시간 위경도",
                                        value = String.format(Locale.US, "%.4f, %.4f", latitude, longitude),
                                        color = Color(0xFFE040FB)
                                    )
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "🌡️",
                                        label = "현재 기온",
                                        value = "${currentTemp.toInt()}°C",
                                        color = Color(0xFF00E676),
                                        progress = ((currentTemp + 15) / 60f).toFloat(),
                                        progressColor = Color(0xFF00E676)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "🥵",
                                        label = "체감 온도",
                                        value = "${feelsLikeTemp.toInt()}°C",
                                        color = Color(0xFFFF5252),
                                        progress = ((feelsLikeTemp + 15) / 60f).toFloat(),
                                        progressColor = Color(0xFFFF5252)
                                    )
                                    
                                    val tempSpreadProgress = if (maxTemp > minTemp) {
                                        ((currentTemp - minTemp) / (maxTemp - minTemp)).toFloat()
                                    } else {
                                        0.5f
                                    }
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "📈",
                                        label = "최고 / 최저",
                                        value = "${maxTemp.toInt()}°C / ${minTemp.toInt()}°C",
                                        color = Color(0xFFFFB300),
                                        progress = tempSpreadProgress,
                                        progressColor = Color(0xFFFFB300)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "💧",
                                        label = "대기 습도",
                                        value = "$humidity%",
                                        color = Color(0xFF00B0FF),
                                        progress = humidity / 100f,
                                        progressColor = Color(0xFF00B0FF)
                                    )
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "🌀",
                                        label = "평균 풍속",
                                        value = "${windSpeed} m/s",
                                        color = Color(0xFF00E5FF),
                                        progress = (windSpeed / 15f).toFloat(),
                                        progressColor = Color(0xFF00E5FF),
                                        isRotating = windSpeed > 0
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "🎈",
                                        label = "대기 압력",
                                        value = "${pressure} hPa",
                                        color = Color(0xFFB0BEC5),
                                        progress = ((pressure - 950) / 100f).toFloat(),
                                        progressColor = Color(0xFFB0BEC5)
                                    )
                                    
                                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                    val timeOfDayKorean = when (hour) {
                                        in 6..11 -> "아침"
                                        in 12..17 -> "오후"
                                        in 18..21 -> "저녁"
                                        else -> "밤"
                                    }
                                    GridDetailItem(
                                        modifier = Modifier.weight(1f),
                                        emoji = "⏰",
                                        label = "관측 시간대",
                                        value = timeOfDayKorean,
                                        color = Color(0xFFE040FB),
                                        progress = hour / 23f,
                                        progressColor = Color(0xFFE040FB)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            // 하단 돌아가기 버튼
                            Button(
                                onClick = { showWeatherDetails = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = "돌아가기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 설정창 열기
    if (showSettings) {
        SettingsDialog(
            apiKeyManager = apiKeyManager,
            onDismiss = { showSettings = false },
            onSave = { showSettings = false }
        )
    }
}

@Composable
fun GridDetailItem(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    color: Color,
    progress: Float? = null,
    progressColor: Color = color,
    isRotating: Boolean = false
) {
    // 회전 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "IconRotation")
    val rotationAngle by if (isRotating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Rotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = emoji,
                    fontSize = 15.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotationAngle
                    }
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (progress != null) {
                val clampedProgress = progress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(clampedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        progressColor.copy(alpha = 0.6f),
                                        progressColor
                                    )
                                ),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
