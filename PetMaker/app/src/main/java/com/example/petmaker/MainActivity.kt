package com.example.petmaker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.data.local.PetConstants
import com.example.petmaker.data.remote.PetCreationData
import com.example.petmaker.data.remote.RetrofitClient
import com.example.petmaker.ui.screens.CollectionScreen
import com.example.petmaker.ui.screens.GenerationScreen
import com.example.petmaker.ui.screens.MainScreen
import com.example.petmaker.ui.screens.ResultScreen
import com.example.petmaker.ui.theme.PetMakerTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random as KRandom

class MainActivity : ComponentActivity() {
    private val apiKeyManager by lazy { ApiKeyManager(this) }

    /** 두 GPS 좌표 사이의 거리를 미터 단위로 계산 (Haversine 공식) */
    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Maps SDK 사전 비동기 초기화 (웜업)
        com.google.android.gms.maps.MapsInitializer.initialize(
            applicationContext,
            com.google.android.gms.maps.MapsInitializer.Renderer.LATEST
        ) {}

        enableEdgeToEdge()
        setContent {
            PetMakerTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
                val appScope = rememberCoroutineScope()

                // ── 실시간 기기 및 날씨 상태 ──────────────────────────────────
                var lat by remember { mutableStateOf(37.57) }
                var lon by remember { mutableStateOf(126.97) }
                var currentWeather by remember { mutableStateOf("맑음") }
                var currentTemp by remember { mutableStateOf(20.0) }
                var currentLocation by remember { mutableStateOf("서울 종로구") }
                var feelsLikeTemp by remember { mutableStateOf(20.0) }
                var minTemp by remember { mutableStateOf(15.0) }
                var maxTemp by remember { mutableStateOf(25.0) }
                var humidity by remember { mutableStateOf(50) }
                var pressure by remember { mutableStateOf(1013) }
                var windSpeed by remember { mutableStateOf(2.5) }

                // ── 걸음 거리 / 위치 추적 ─────────────────────────────────────
                var todayDistance by remember { mutableStateOf(apiKeyManager.todayWalkedDistance) }
                var previousLatLng by remember { mutableStateOf<LatLng?>(null) }
                var hasReceivedGps by remember { mutableStateOf(false) }

                // ── 가상 설정 ─────────────────────────────────────────────────
                var mockWeather by remember { mutableStateOf(apiKeyManager.mockWeather) }
                var mockTimezone by remember { mutableStateOf(apiKeyManager.mockTimezone) }

                // ── 펫 생성 임시 전달 상태 ────────────────────────────────────
                var generatedPetData by remember { mutableStateOf<PetCreationData?>(null) }
                var generatedImagePath by remember { mutableStateOf("") }

                // ── 포탈 스폰 상태 ────────────────────────────────────────────
                var spawnSpots by remember { mutableStateOf<List<LatLng>>(emptyList()) }
                var lastGeneratedLoc by remember { mutableStateOf<LatLng?>(null) }
                var activePortal by remember { mutableStateOf<LatLng?>(null) }

                // ── 날씨 데이터 갱신 ──────────────────────────────────────────
                fun fetchWeather() {
                    val key = apiKeyManager.weatherApiKey
                    if (key.isEmpty()) return
                    appScope.launch(Dispatchers.IO) {
                        try {
                            val response = RetrofitClient.getWeatherApi()
                                .getCurrentWeather(lat, lon, key)
                                .execute()
                            if (response.isSuccessful) {
                                val body = response.body() ?: return@launch
                                withContext(Dispatchers.Main) {
                                    currentWeather = body.weather.firstOrNull()?.description ?: "맑음"
                                    currentTemp = body.main.temp
                                    feelsLikeTemp = body.main.feelsLike
                                    minTemp = body.main.tempMin
                                    maxTemp = body.main.tempMax
                                    humidity = body.main.humidity
                                    pressure = body.main.pressure
                                    windSpeed = body.wind?.speed ?: 0.0
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // ── 주소명 업데이트 ───────────────────────────────────────────
                fun updateAddress(latitude: Double, longitude: Double) {
                    appScope.launch(Dispatchers.IO) {
                        val name = try {
                            val geocoder = Geocoder(context, Locale.KOREA)
                            val addr = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                            val admin = addr?.subLocality ?: addr?.locality ?: ""
                            val sub = addr?.thoroughfare ?: addr?.subThoroughfare ?: ""
                            when {
                                admin.isNotEmpty() || sub.isNotEmpty() -> "$admin $sub".trim()
                                addr != null -> addr.getAddressLine(0) ?: "알 수 없는 위치"
                                else -> "종로구"
                            }
                        } catch (e: Exception) {
                            "종로구"
                        }
                        withContext(Dispatchers.Main) { currentLocation = name }
                    }
                }

                // ── 위치 콜백 ─────────────────────────────────────────────────
                val locationCallback = remember {
                    object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            val loc = result.lastLocation ?: return
                            val curr = LatLng(loc.latitude, loc.longitude)
                            previousLatLng?.let { prev ->
                                val dist = distanceMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
                                // 2m~200m 사이 타당한 움직임만 누적 (GPS 튀는 현상 방지)
                                if (dist in 2.0..200.0) {
                                    val today = todayDateString()
                                    if (apiKeyManager.lastWalkedDate != today) {
                                        apiKeyManager.lastWalkedDate = today
                                        apiKeyManager.todayWalkedDistance = 0f
                                    }
                                    val newDist = apiKeyManager.todayWalkedDistance + dist.toFloat()
                                    apiKeyManager.todayWalkedDistance = newDist
                                    todayDistance = newDist
                                }
                            }
                            previousLatLng = curr
                            lat = loc.latitude
                            lon = loc.longitude
                            hasReceivedGps = true
                            updateAddress(loc.latitude, loc.longitude)
                            fetchWeather()
                        }
                    }
                }

                // ── 위치 업데이트 시작/중지 ───────────────────────────────────
                fun startLocationUpdates() {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) return
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                lat = loc.latitude
                                lon = loc.longitude
                                hasReceivedGps = true
                                updateAddress(loc.latitude, loc.longitude)
                                fetchWeather()
                            }
                        }
                        val req = com.google.android.gms.location.LocationRequest.Builder(
                            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000L
                        ).setMinUpdateIntervalMillis(2000L).build()

                        fusedLocationClient.requestLocationUpdates(req, locationCallback, android.os.Looper.getMainLooper())
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
                }

                // ── 권한 런처 ─────────────────────────────────────────────────
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    ) {
                        startLocationUpdates()
                    } else {
                        Toast.makeText(context, "위치 권한이 없어 기본 위치(서울 종로구)로 로드합니다.", Toast.LENGTH_SHORT).show()
                        fetchWeather()
                    }
                }

                // ── 최초 실행 초기화 ──────────────────────────────────────────
                LaunchedEffect(Unit) {
                    // 날짜가 바뀌면 오늘 걸음 거리 초기화
                    val today = todayDateString()
                    if (apiKeyManager.lastWalkedDate != today) {
                        apiKeyManager.lastWalkedDate = today
                        apiKeyManager.todayWalkedDistance = 0f
                        todayDistance = 0f
                    }
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!hasFine && !hasCoarse) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    } else {
                        startLocationUpdates()
                    }
                }

                // 5분마다 날씨 주기적 갱신
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(300_000L)
                        fetchWeather()
                    }
                }

                // ── 포탈 스폰 로직 ────────────────────────────────────────────
                LaunchedEffect(lat, lon, hasReceivedGps) {
                    if (!hasReceivedGps) return@LaunchedEffect
                    val curr = LatLng(lat, lon)
                    val last = lastGeneratedLoc
                    if (last == null) {
                        // 최초 1~2개 무작위 생성
                        val rng = Random(System.currentTimeMillis())
                        val count = rng.nextInt(2) + 1
                        spawnSpots = List(count) {
                            LatLng(curr.latitude + (rng.nextDouble() - 0.5) * 0.0016,
                                   curr.longitude + (rng.nextDouble() - 0.5) * 0.0016)
                        }
                        lastGeneratedLoc = curr
                    } else if (distanceMeters(curr.latitude, curr.longitude, last.latitude, last.longitude) >= 15.0) {
                        lastGeneratedLoc = curr
                        if (spawnSpots.size < 3 && KRandom.nextDouble() < 0.3) {
                            val rng = Random(System.currentTimeMillis())
                            spawnSpots = spawnSpots + LatLng(
                                curr.latitude + (rng.nextDouble() - 0.5) * 0.0016,
                                curr.longitude + (rng.nextDouble() - 0.5) * 0.0016
                            )
                        }
                    }
                }

                // ── 네비게이션 ────────────────────────────────────────────────
                NavHost(navController = navController, startDestination = "main") {

                    composable("main") {
                        val activeWeather = if (mockWeather == "API") currentWeather else mockWeather
                        MainScreen(
                            apiKeyManager = apiKeyManager,
                            currentWeather = activeWeather,
                            mockTimezone = mockTimezone,
                            currentTemp = currentTemp,
                            currentLocation = currentLocation,
                            feelsLikeTemp = feelsLikeTemp,
                            minTemp = minTemp,
                            maxTemp = maxTemp,
                            humidity = humidity,
                            pressure = pressure,
                            windSpeed = windSpeed,
                            latitude = lat,
                            longitude = lon,
                            spawnSpots = spawnSpots,
                            todayDistance = todayDistance,
                            onNavigateToCollection = { autoOpen ->
                                navController.navigate("collection?autoOpenPartner=$autoOpen")
                            },
                            onGeneratePet = { portal ->
                                activePortal = portal
                                navController.navigate("generation")
                            },
                            onRefreshSettings = {
                                mockWeather = apiKeyManager.mockWeather
                                mockTimezone = apiKeyManager.mockTimezone
                            }
                        )
                    }

                    composable("generation") {
                        val activeWeather = if (mockWeather == "API") currentWeather else mockWeather
                        GenerationScreen(
                            apiKeyManager = apiKeyManager,
                            weatherDesc = activeWeather,
                            weatherMain = PetConstants.weatherDescToMain(activeWeather),
                            temp = currentTemp,
                            addressName = currentLocation,
                            onGenerationSuccess = { petData, imgPath ->
                                generatedPetData = petData
                                generatedImagePath = imgPath
                                navController.navigate("result") {
                                    popUpTo("generation") { inclusive = true }
                                }
                            },
                            onGenerationFailure = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                navController.navigate("main") {
                                    popUpTo("generation") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("result") {
                        val petData = generatedPetData
                        if (petData != null) {
                            val activeWeather = if (mockWeather == "API") currentWeather else mockWeather
                            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            val timezone = when (hour) {
                                in 6..11 -> "Morning"
                                in 12..17 -> "Afternoon"
                                in 18..21 -> "Evening"
                                else -> "Night"
                            }
                            ResultScreen(
                                petData = petData,
                                tempImagePath = generatedImagePath,
                                weather = activeWeather,
                                temperature = currentTemp,
                                timezone = timezone,
                                location = currentLocation,
                                onSaveSuccess = {
                                    activePortal?.let { spawnSpots = spawnSpots - it }
                                    activePortal = null
                                    navController.navigate("main") {
                                        popUpTo("result") { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    activePortal = null
                                    navController.navigate("main") {
                                        popUpTo("result") { inclusive = true }
                                    }
                                }
                            )
                        } else {
                            navController.navigate("main")
                        }
                    }

                    composable(
                        route = "collection?autoOpenPartner={autoOpenPartner}",
                        arguments = listOf(
                            androidx.navigation.navArgument("autoOpenPartner") {
                                type = androidx.navigation.NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStack ->
                        CollectionScreen(
                            apiKeyManager = apiKeyManager,
                            autoOpenPartner = backStack.arguments?.getBoolean("autoOpenPartner") ?: false,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}