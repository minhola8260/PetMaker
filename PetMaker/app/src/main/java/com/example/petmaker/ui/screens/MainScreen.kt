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
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.asFlow
import coil.compose.AsyncImage
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.ui.components.SettingsDialog
import com.example.petmaker.ui.components.WeatherBackground
import com.example.petmaker.ui.components.WeatherType
import com.example.petmaker.ui.theme.MapStyles
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    apiKeyManager: ApiKeyManager,
    currentWeather: String, // 맑음, 비, 눈, 흐림
    mockTimezone: String, // 가상 시간대 (API, 아침, 오후, 저녁, 밤)
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
    spawnSpots: List<LatLng>, // 외부에서 주입되는 포탈 목록
    todayDistance: Float,
    onNavigateToCollection: (Boolean) -> Unit, // Boolean 파라미터로 도감 내 파트너 자동 오픈 구분
    onGeneratePet: (LatLng) -> Unit, // 발견한 특정 포탈 좌표를 리턴하도록 수정
    onRefreshSettings: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showWeatherDetails by remember { mutableStateOf(false) }
    var currentTimeText by remember { mutableStateOf("") }
 
    val context = LocalContext.current
    val petDao = remember { PetDatabase.getInstance(context).petDao() }
    
    // LiveData.observeAsState 대신 의존성 추가 없이 Flow로 변환하여 collectAsState 활용
    val partnerPet by petDao.getPartnerPetLiveData().asFlow().collectAsState(initial = null)
 
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
 
    // 2. 날씨 한글에 따른 매핑
    val weatherType = remember(currentWeather) {
        when {
            currentWeather.contains("비") -> WeatherType.RAIN
            currentWeather.contains("눈") -> WeatherType.SNOW
            currentWeather.contains("흐림") || currentWeather.contains("구름") || currentWeather.contains("안개") -> WeatherType.CLOUDS
            else -> WeatherType.CLEAR
        }
    }
 
    // 시간대에 따른 텍스트 매핑 (현재 시각 또는 가상 시간대 기준)
    val timeOfDayText = remember(currentTimeText, mockTimezone) {
        if (mockTimezone != "API") {
            when (mockTimezone) {
                "아침" -> "☀️ Morning"
                "오후" -> "☀️ Afternoon"
                "저녁" -> "🌙 Evening"
                "밤" -> "🌙 Night"
                else -> "☀️ Morning"
            }
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 6..11 -> "☀️ Morning"
                in 12..17 -> "☀️ Afternoon"
                in 18..21 -> "🌙 Evening"
                else -> "🌙 Night"
            }
        }
    }
 
    // 3. 지도 및 카메라 위치 상태 관리
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 17f)
    }
 
    val coroutineScope = rememberCoroutineScope()
    var userInteracted by remember { mutableStateOf(false) }
    var returnJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 지도를 움직인 뒤 3초간 추가 움직임(동작)이 없으면 본인 위치로 부드럽게 되돌아가도록 처리
    LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.cameraMoveStartedReason) {
        val isMoving = cameraPositionState.isMoving
        val reason = cameraPositionState.cameraMoveStartedReason
        if (isMoving && reason == CameraMoveStartedReason.GESTURE) {
            userInteracted = true
            returnJob?.cancel()
            returnJob = null
        } else if (!isMoving && userInteracted) {
            delay(3000)
            userInteracted = false
            returnJob?.cancel()
            returnJob = coroutineScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)),
                    1000
                )
            }
        }
    }

    // 사용자 위치가 변할 때 부드럽게 카메라 이동 (유저가 지도 조작 중이 아닐 때만 작동하여 조작 방해 차단)
    LaunchedEffect(latitude, longitude) {
        if (!userInteracted) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), cameraPositionState.position.zoom),
                1000
            )
        }
    }
 
    // 낮/밤 시간대 판단 (가상 시간대 반영)
    val isNight = remember(currentTimeText, mockTimezone) {
        if (mockTimezone != "API") {
            mockTimezone == "저녁" || mockTimezone == "밤"
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 18 || hour < 6
        }
    }
 
    // 기상 상황 및 시간대에 최적화된 지도 테마 결정
    val mapStyleJson = remember(currentWeather, isNight) {
        when {
            currentWeather.contains("비") -> MapStyles.RAINY
            currentWeather.contains("눈") -> MapStyles.SNOWY
            currentWeather.contains("흐림") || currentWeather.contains("구름") || currentWeather.contains("안개") -> MapStyles.CLOUDY
            isNight -> MapStyles.NIGHT
            else -> MapStyles.DAY
        }
    }
 
    val mapProperties = remember(mapStyleJson) {
        MapProperties(
            mapStyleOptions = MapStyleOptions(mapStyleJson),
            maxZoomPreference = 20f,
            minZoomPreference = 15f
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            zoomGesturesEnabled = true,
            scrollGesturesEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    }
 
    // 두 지점 간의 거리(m) 계산 공식 (Haversine)
    fun getDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // 지구 반지름 (m)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
 
    // 포탈 40m 내에 도달했는지 여부 감지 (펫 발견 트리거)
    val isNearPortal = remember(latitude, longitude, spawnSpots) {
        spawnSpots.any { spot ->
            getDistanceInMeters(latitude, longitude, spot.latitude, spot.longitude) <= 40.0
        }
    }
 
    // UI 숨김 상태 관리 (지도를 넓게 감상하는 뷰 모드)
    var isUiHidden by remember { mutableStateOf(false) }
 
    // 포탈 파동 효과 애니메이션 무한 트랜지션
    val pulseTransition = rememberInfiniteTransition(label = "PortalPulse")
    val portalPulseRadius by pulseTransition.animateFloat(
        initialValue = 10f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "portalPulseRadius"
    )
    val portalPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "portalPulseAlpha"
    )
 
    // 펫 발견 버튼용 무한 펄스(스케일) 애니메이션
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

    // 모험가 펫 감지 범위 레이더 스캔 애니메이션 무한 트랜지션
    val radarTransition = rememberInfiniteTransition(label = "RadarPulse")
    val radarRadius by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRadius"
    )
    val radarAlpha by radarTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )
 
    WeatherBackground(
        weatherType = weatherType,
        mapContent = {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (showWeatherDetails) 16.dp else 0.dp),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = {
                    isUiHidden = !isUiHidden
                }
            ) {
                // 1. 모험가 위치 40m 상호작용 레이저 링 (실제 미터 반경이므로 Circle 유지)
                Circle(
                    center = LatLng(latitude, longitude),
                    radius = 40.0,
                    fillColor = Color(0xFF00B0FF).copy(alpha = 0.05f),
                    strokeColor = Color(0xFF00B0FF).copy(alpha = 0.2f),
                    strokeWidth = 1.5f
                )

                // 2. 모험가 위치 레이더 감지 범위 퍼지는 효과 (히트 스캔 감지 시각화)
                Circle(
                    center = LatLng(latitude, longitude),
                    radius = radarRadius.toDouble(),
                    fillColor = Color(0xFF00B0FF).copy(alpha = radarAlpha * 0.12f),
                    strokeColor = Color(0xFF00B0FF).copy(alpha = radarAlpha * 0.7f),
                    strokeWidth = 2f
                )

                // 3. 모험가 위치 입체 레이더 및 서클 마커 (이전과 같이 줌 확대되지 않도록 그대로 유지)
                // 플레이어 하단 그림자 (Drop Shadow)
                Circle(
                    center = LatLng(latitude, longitude),
                    radius = 5.0,
                    fillColor = Color.Black.copy(alpha = 0.35f),
                    strokeColor = Color.Transparent,
                    strokeWidth = 0f
                )
                // 모험가 코어 입체 서클 (Inner Glow)
                Circle(
                    center = LatLng(latitude, longitude),
                    radius = 3.8,
                    fillColor = Color(0xFF00B0FF),
                    strokeColor = Color.White,
                    strokeWidth = 3f
                )
                // 모험가 하이라이트 코어
                Circle(
                    center = LatLng(latitude, longitude),
                    radius = 1.2,
                    fillColor = Color.White.copy(alpha = 0.9f),
                    strokeColor = Color.Transparent,
                    strokeWidth = 0f
                )

                // 4. 펫 스폰 포탈 입체 소환진 렌더링 (줌 연동 스케일링 - 초기화 시 크기 튐 방지)
                val zoom = cameraPositionState.position.zoom
                val stableZoom = if (zoom < 15f) 17f else zoom
                val markerScale = (1.0f + (stableZoom - 17f) * 0.3f).coerceIn(0.4f, 2.5f)
                
                spawnSpots.forEach { spot ->
                    key(spot) {
                        val portalColor = Color(0xFFE040FB)
                        MarkerComposable(
                            state = rememberMarkerState(position = spot),
                            anchor = Offset(0.5f, 0.5f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer {
                                        scaleX = markerScale
                                        scaleY = markerScale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 아우터 포탈 링
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(portalColor.copy(alpha = 0.15f))
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                portalColor.copy(alpha = 0.7f)
                                            ),
                                            CircleShape
                                        )
                                )
                                // 이너 포탈 링 (펄스 파동 적용)
                                Box(
                                    modifier = Modifier
                                        .size(portalPulseRadius.dp)
                                        .clip(CircleShape)
                                        .background(portalColor.copy(alpha = portalPulseAlpha))
                                )
                                // 포탈 코어 구체 및 펫 흔적 (🐾 마크 추가)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(portalColor)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🐾",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val topPadding by animateDpAsState(
                targetValue = if (isUiHidden) 24.dp else 88.dp,
                animationSpec = tween(300),
                label = "topPadding"
            )

            // 1. 상단 타이틀바 및 오늘 이동 거리량 표시 (요구사항 2: 투명도를 0.35f -> 0.2f 로 낮추어 배경 시인성 극대화)
            AnimatedVisibility(
                visible = !isUiHidden,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            containerColor = Color.Black.copy(alpha = 0.2f)
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🏃", fontSize = 14.sp)
                            Text(
                                text = "오늘 이동 거리: ${if (todayDistance >= 1000) String.format(Locale.getDefault(), "%.2f km", todayDistance / 1000f) else String.format(Locale.getDefault(), "%.0fm", todayDistance)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. 중앙 날씨 정보 카드 & 새로운 펫 발견 버튼 통합 영역
            // 요구사항 1: 새로운 펫 발견 버튼을 환경 UI 카드 바로 아래에 배치
            // 버튼 배치를 고려하여 가중치를 BiasAlignment(0f, -0.35f) -> BiasAlignment(0f, -0.2f) 로 조정하여 세로 구도 균형 최적화
            AnimatedVisibility(
                visible = !isUiHidden,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(BiasAlignment(0f, -0.2f))
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 환경 정보 카드 (요구사항 2: 투명도를 0.55f -> 0.4f 로 낮추어 배경이 더 잘 투명하게 투영되도록 보정)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWeatherDetails = true },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    // 요구사항 1: 새로운 펫 발견 버튼을 환경 UI 카드 바로 아래에 배치 (하단 겹침 차단)
                    // GPS 포탈 스폰 40m 내에 사용자가 도달했을 때 실시간으로 나타납니다.
                    AnimatedVisibility(
                        visible = isNearPortal,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        Button(
                            onClick = {
                                // 사용자와 가장 가까운 포탈 마커를 찾아서 넘김
                                val nearestPortal = spawnSpots.minByOrNull { spot ->
                                    getDistanceInMeters(latitude, longitude, spot.latitude, spot.longitude)
                                }
                                nearestPortal?.let {
                                    onGeneratePet(it)
                                }
                            },
                            modifier = Modifier
                                .height(68.dp) // 대폭 상향된 높이
                                .widthIn(min = 220.dp) // 버튼 가로 사이즈 확장
                                .scale(pulseScale),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE040FB)
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "✨ 새로운 펫 발견!",
                                fontSize = 20.sp, // 더 커진 폰트 사이즈
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. 하단 인터랙션 영역 (가로형 분할 정렬)
            AnimatedVisibility(
                visible = !isUiHidden,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 0.dp, end = 16.dp, bottom = 24.dp) // 왼쪽에 딱 붙도록 start = 0.dp 패딩 설정
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // 요구사항 1: 파트너 표시 UI를 왼쪽에 딱 붙도록 배치 (105.dp 크기 상향)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(105.dp) // 크기 105.dp
                            .clip(CircleShape)
                            .background(Color(0xFF130924).copy(alpha = 0.75f))
                            .border(
                                BorderStroke(
                                    3.dp,
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFE040FB), Color(0xFF00B0FF))
                                    )
                                ),
                                CircleShape
                            )
                            .clickable {
                                // 요구사항 3: 파트너 UI 클릭 시 도감 화면으로 바로 넘어가서 자동 팝업되도록 처리
                                onNavigateToCollection(true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (partnerPet == null) {
                            Text(
                                text = "🥚",
                                fontSize = 48.sp
                            )
                        } else {
                            AsyncImage(
                                model = partnerPet?.getImagePath(), // Getter 적용
                                contentDescription = "Partner Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 도감 열기 버튼 (우측 하단 원형 리스트 아이콘 배치)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF4A148C), Color(0xFF130924))
                                )
                            )
                            .border(2.5.dp, Color(0xFFFFD700), CircleShape) // 프리미엄 골드 컬러링
                            .clickable {
                                // 일반 도감 화면 열기 (autoOpenPartner = false)
                                onNavigateToCollection(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Collection Book",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        } // Box 끝

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
                                
                                val timeOfDayKorean = if (mockTimezone != "API") {
                                    mockTimezone
                                } else {
                                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                    when (hour) {
                                        in 6..11 -> "아침"
                                        in 12..17 -> "오후"
                                        in 18..21 -> "저녁"
                                        else -> "밤"
                                    }
                                }
                                val progressHour = if (mockTimezone != "API") {
                                    when (mockTimezone) {
                                        "아침" -> 9f
                                        "오후" -> 15f
                                        "저녁" -> 19f
                                        "밤" -> 23f
                                        else -> 9f
                                    }
                                } else {
                                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toFloat()
                                }
                                GridDetailItem(
                                    modifier = Modifier.weight(1f),
                                    emoji = "⏰",
                                    label = "관측 시간대",
                                    value = timeOfDayKorean,
                                    color = Color(0xFFE040FB),
                                    progress = progressHour / 23f,
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

    // 설정창 열기
    if (showSettings) {
        SettingsDialog(
            apiKeyManager = apiKeyManager,
            onDismiss = { showSettings = false },
            onSave = { 
                showSettings = false 
                onRefreshSettings()
            },
            onTestGenerate = {
                showSettings = false
                // 현재 사용자 위치로 가상 펫 강제 생성 프로세스(API 호출 및 결과 화면 연동) 구동
                onGeneratePet(LatLng(latitude, longitude))
            }
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
