package com.example.petmaker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.random.Random

// 날씨 상태 정의
enum class WeatherType {
    RAIN, SNOW, CLOUDS, CLEAR
}

// 빗방울 입자
private data class Raindrop(
    var x: Float,
    var y: Float,
    val speed: Float,
    val length: Float,
    val width: Float
)

// 눈송이 입자
private data class Snowflake(
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    var angle: Float,
    val swingSpeed: Float
)

@Composable
fun WeatherBackground(
    weatherType: WeatherType,
    modifier: Modifier = Modifier,
    mapContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // 배경 그라디언트 컬러 구성
    val (startColor, endColor) = remember(weatherType) {
        when (weatherType) {
            WeatherType.RAIN -> Color(0xFFFF8A65) to Color(0xFF8E24AA) // 오렌지 -> 핑크/퍼플 (UI_1.png)
            WeatherType.SNOW -> Color(0xFF1A237E) to Color(0xFFE91E63) // 딥네이비 -> 버건디/핑크 (UI_2.png)
            WeatherType.CLOUDS -> Color(0xFF4FC3F7) to Color(0xFFD4E157) // 스카이블루 -> 라임 (UI_3.png)
            WeatherType.CLEAR -> Color(0xFF00B0FF) to Color(0xFF80DEEA) // 네온블루 -> 밝은하늘 (UI_4.png)
        }
    }

    // 날씨 상태 변경 시 부드럽게 색상 전환 (1초 동안 트랜지션)
    val animatedStartColor by animateColorAsState(targetValue = startColor, animationSpec = tween(1000), label = "StartColor")
    val animatedEndColor by animateColorAsState(targetValue = endColor, animationSpec = tween(1000), label = "EndColor")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedStartColor, animatedEndColor)
                )
            )
    ) {
        // 지도 콘텐츠 (날씨 파티클 아래 레이어)
        mapContent?.invoke()

        // 날씨 파티클 애니메이션
        when (weatherType) {
            WeatherType.RAIN -> RainAnimation()
            WeatherType.SNOW -> SnowAnimation()
            WeatherType.CLOUDS -> CloudAnimation()
            else -> {}
        }

        // UI 콘텐츠 (최상단 레이어)
        content()
    }
}

@Composable
private fun RainAnimation() {
    val raindrops = remember { mutableStateListOf<Raindrop>() }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1080, 2200)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val wInt = width.toInt()
        val hInt = height.toInt()
        if (canvasSize.width != wInt || canvasSize.height != hInt) {
            canvasSize = androidx.compose.ui.unit.IntSize(wInt, hInt)
        }

        // 초기화 (빗방울 20개)
        if (raindrops.isEmpty()) {
            for (i in 0 until 20) {
                raindrops.add(
                    Raindrop(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() * height,
                        speed = 18f + Random.nextFloat() * 15f,
                        length = 30f + Random.nextFloat() * 30f,
                        width = 1.5f + Random.nextFloat() * 1.5f
                    )
                )
            }
        }

        // 입자 그리기
        raindrops.forEach { drop ->
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(drop.x, drop.y),
                end = Offset(drop.x, drop.y + drop.length),
                strokeWidth = drop.width
            )
        }
    }

    // 프레임 주기마다 위치 갱신 (Compose Redraw 트리거를 위해 객체를 copy하여 교체)
    LaunchedEffect(canvasSize) {
        val limitY = canvasSize.height.toFloat().takeIf { it > 0 } ?: 2200f
        val limitX = canvasSize.width.toFloat().takeIf { it > 0 } ?: 1200f
        while (isActive) {
            withFrameMillis {
                for (index in raindrops.indices) {
                    val drop = raindrops[index]
                    val nextY = drop.y + drop.speed
                    if (nextY > limitY) {
                        raindrops[index] = drop.copy(
                            x = Random.nextFloat() * limitX,
                            y = -100f
                        )
                    } else {
                        raindrops[index] = drop.copy(y = nextY)
                    }
                }
            }
            yield()
        }
    }
}

@Composable
private fun SnowAnimation() {
    val snowflakes = remember { mutableStateListOf<Snowflake>() }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1080, 2200)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val wInt = width.toInt()
        val hInt = height.toInt()
        if (canvasSize.width != wInt || canvasSize.height != hInt) {
            canvasSize = androidx.compose.ui.unit.IntSize(wInt, hInt)
        }

        // 초기화 (눈송이 30개)
        if (snowflakes.isEmpty()) {
            for (i in 0 until 30) {
                snowflakes.add(
                    Snowflake(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() * height,
                        speed = 2f + Random.nextFloat() * 3f,
                        radius = 5f + Random.nextFloat() * 5f,
                        angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                        swingSpeed = 0.02f + Random.nextFloat() * 0.03f
                    )
                )
            }
        }

        // 눈송이 그리기
        snowflakes.forEach { flake ->
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = flake.radius,
                center = Offset(flake.x, flake.y)
            )
        }
    }

    // 프레임 갱신 (Compose Redraw 트리거를 위해 객체를 copy하여 교체)
    LaunchedEffect(canvasSize) {
        val limitY = canvasSize.height.toFloat().takeIf { it > 0 } ?: 2200f
        val limitX = canvasSize.width.toFloat().takeIf { it > 0 } ?: 1200f
        while (isActive) {
            withFrameMillis {
                for (index in snowflakes.indices) {
                    val flake = snowflakes[index]
                    val nextY = flake.y + flake.speed
                    val nextAngle = flake.angle + flake.swingSpeed
                    val nextX = flake.x + Math.sin(nextAngle.toDouble()).toFloat() * 1.2f
                    if (nextY > limitY) {
                        snowflakes[index] = flake.copy(
                            x = Random.nextFloat() * limitX,
                            y = -50f,
                            angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        )
                    } else {
                        snowflakes[index] = flake.copy(x = nextX, y = nextY, angle = nextAngle)
                    }
                }
            }
            yield()
        }
    }
}

// 구름 입자
private data class Cloud(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
)

@Composable
private fun CloudAnimation() {
    val clouds = remember { mutableStateListOf<Cloud>() }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1080, 2200)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val wInt = width.toInt()
        val hInt = height.toInt()
        if (canvasSize.width != wInt || canvasSize.height != hInt) {
            canvasSize = androidx.compose.ui.unit.IntSize(wInt, hInt)
        }

        // 초기화 (구름 6개)
        if (clouds.isEmpty()) {
            for (i in 0 until 6) {
                clouds.add(
                    Cloud(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() * (height * 0.7f), // 화면 상중단 영역에 무작위 배치
                        speed = 0.5f + Random.nextFloat() * 1.0f,
                        size = 150f + Random.nextFloat() * 150f,
                        alpha = 0.08f + Random.nextFloat() * 0.12f
                    )
                )
            }
        }

        // 구름 그리기 (반투명 원 여러 개가 겹치는 부드러운 형태로 그림)
        clouds.forEach { cloud ->
            // 구름 한 개당 3개의 원을 겹쳐서 자연스러운 구름 모양 생성
            drawCircle(
                color = Color.White.copy(alpha = cloud.alpha),
                radius = cloud.size,
                center = Offset(cloud.x, cloud.y)
            )
            drawCircle(
                color = Color.White.copy(alpha = cloud.alpha * 0.8f),
                radius = cloud.size * 0.8f,
                center = Offset(cloud.x + cloud.size * 0.5f, cloud.y - cloud.size * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = cloud.alpha * 0.8f),
                radius = cloud.size * 0.7f,
                center = Offset(cloud.x - cloud.size * 0.5f, cloud.y + cloud.size * 0.1f)
            )
        }
    }

    // 프레임 주기마다 구름을 오른쪽으로 천천히 이동시킴
    LaunchedEffect(canvasSize) {
        val limitY = canvasSize.height.toFloat().takeIf { it > 0 } ?: 2200f
        val limitX = canvasSize.width.toFloat().takeIf { it > 0 } ?: 1200f
        while (isActive) {
            withFrameMillis {
                for (index in clouds.indices) {
                    val cloud = clouds[index]
                    var nextX = cloud.x + cloud.speed
                    // 화면 오른쪽을 벗어나면 왼쪽으로 복귀 (구름 크기를 고려하여 여유값 추가)
                    if (nextX > limitX + cloud.size * 1.5f) {
                        clouds[index] = cloud.copy(
                            x = -cloud.size * 1.5f,
                            y = Random.nextFloat() * (limitY * 0.7f)
                        )
                    } else {
                        clouds[index] = cloud.copy(x = nextX)
                    }
                }
            }
            yield()
        }
    }
}
