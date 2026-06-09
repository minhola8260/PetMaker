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
import com.example.petmaker.data.remote.PetCreationData
import com.example.petmaker.data.remote.RetrofitClient
import com.example.petmaker.ui.screens.CollectionScreen
import com.example.petmaker.ui.screens.GenerationScreen
import com.example.petmaker.ui.screens.MainScreen
import com.example.petmaker.ui.screens.ResultScreen
import com.example.petmaker.ui.theme.PetMakerTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : ComponentActivity() {
    private val apiKeyManager by lazy { ApiKeyManager(this) }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMakerTheme {
                val navController = rememberNavController()

                // 실시간 기기 및 날씨 상태 값들
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

                val context = LocalContext.current
                val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

                // 펫 생성 임시 전달 상태
                var generatedPetData by remember { mutableStateOf<PetCreationData?>(null) }
                var generatedImagePath by remember { mutableStateOf("") }

                // 실시간 날씨 데이터 수집 함수
                fun fetchWeather() {
                    if (apiKeyManager.weatherApiKey.isEmpty()) return
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val weatherResponse = RetrofitClient.getWeatherApi()
                                .getCurrentWeather(lat, lon, apiKeyManager.weatherApiKey)
                                .execute()
                            if (weatherResponse.isSuccessful && weatherResponse.body() != null) {
                                val body = weatherResponse.body()!!
                                val desc = body.weather.firstOrNull()?.description ?: "맑음"
                                val temp = body.main.temp
                                val feelsLike = body.main.feelsLike
                                val tMin = body.main.tempMin
                                val tMax = body.main.tempMax
                                val hum = body.main.humidity
                                val press = body.main.pressure
                                val wind = body.wind?.speed ?: 0.0
                                withContext(Dispatchers.Main) {
                                    currentWeather = desc
                                    currentTemp = temp
                                    feelsLikeTemp = feelsLike
                                    minTemp = tMin
                                    maxTemp = tMax
                                    humidity = hum
                                    pressure = press
                                    windSpeed = wind
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // 위치 명칭 업데이트 함수
                fun updateAddressName(latitude: Double, longitude: Double) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val addressName = try {
                            val geocoder = Geocoder(context, Locale.KOREA)
                            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
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
                        withContext(Dispatchers.Main) {
                            currentLocation = addressName
                        }
                    }
                }

                // 위치 정보 획득 함수
                fun requestDeviceLocation() {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                lat = loc.latitude
                                lon = loc.longitude
                                updateAddressName(loc.latitude, loc.longitude)
                                fetchWeather()
                            } else {
                                fetchWeather()
                            }
                        }
                    }
                }

                // 위치 권한 요청 런처
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                    if (fineGranted || coarseGranted) {
                        requestDeviceLocation()
                    } else {
                        Toast.makeText(context, "위치 권한이 없어 기본 위치(서울 종로구)로 로드합니다.", Toast.LENGTH_SHORT).show()
                        fetchWeather()
                    }
                }

                // 최초 실행 시 위치 권한 요청 및 데이터 동기화
                LaunchedEffect(Unit) {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!hasFine && !hasCoarse) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        requestDeviceLocation()
                    }
                }

                // 날씨 API 키 기입 시 실시간 데이터 리로드
                LaunchedEffect(apiKeyManager.weatherApiKey) {
                    if (apiKeyManager.weatherApiKey.isNotEmpty()) {
                        fetchWeather()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    // 1. 메인 화면
                    composable("main") {
                        MainScreen(
                            apiKeyManager = apiKeyManager,
                            currentWeather = currentWeather,
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
                            onNavigateToCollection = {
                                navController.navigate("collection")
                            },
                            onGeneratePet = {
                                navController.navigate("generation")
                            }
                        )
                    }

                    // 2. 로딩 및 AI 생성 화면
                    composable("generation") {
                        GenerationScreen(
                            apiKeyManager = apiKeyManager,
                            lat = lat,
                            lon = lon,
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

                    // 3. 생성 완료 결과 화면
                    composable("result") {
                        val petData = generatedPetData
                        if (petData != null) {
                            ResultScreen(
                                petData = petData,
                                tempImagePath = generatedImagePath,
                                weather = currentWeather,
                                temperature = currentTemp,
                                timezone = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                                    in 6..11 -> "Morning"
                                    in 12..17 -> "Afternoon"
                                    in 18..21 -> "Evening"
                                    else -> "Night"
                                },
                                location = currentLocation,
                                onSaveSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("result") { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    navController.navigate("main") {
                                        popUpTo("result") { inclusive = true }
                                    }
                                }
                            )
                        } else {
                            navController.navigate("main")
                        }
                    }

                    // 4. 수집 펫 도감 화면
                    composable("collection") {
                        CollectionScreen(
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}