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
        // 날씨 파티클 애니메이션
        when (weatherType) {
            WeatherType.RAIN -> RainAnimation()
            WeatherType.SNOW -> SnowAnimation()
            else -> {}
        }
        content()
    }
}

@Composable
private fun RainAnimation() {
    val raindrops = remember { mutableStateListOf<Raindrop>() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

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

    // 프레임 주기마다 위치 갱신
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                raindrops.forEachIndexed { index, drop ->
                    drop.y += drop.speed
                    // 화면 밑으로 나가면 상단에서 재배치
                    if (drop.y > 2200f) {
                        raindrops[index] = drop.copy(
                            x = Random.nextFloat() * 1200f,
                            y = -100f
                        )
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

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

    // 프레임 갱신
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                snowflakes.forEachIndexed { index, flake ->
                    flake.y += flake.speed
                    flake.angle += flake.swingSpeed
                    // 좌우로 흔들리며 하강하는 바람 효과 추가
                    flake.x += Math.sin(flake.angle.toDouble()).toFloat() * 1.2f

                    // 화면 밑으로 나가면 리셋
                    if (flake.y > 2200f) {
                        snowflakes[index] = flake.copy(
                            x = Random.nextFloat() * 1200f,
                            y = -50f,
                            angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        )
                    }
                }
            }
            yield()
        }
    }
}
