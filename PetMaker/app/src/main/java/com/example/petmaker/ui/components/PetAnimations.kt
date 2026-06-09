package com.example.petmaker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 4포인트 스파클 패스 그리기 헬퍼
fun DrawScope.drawSparkle(center: Offset, size: Float, color: Color) {
    val cx = center.x
    val cy = center.y
    val r = size / 2

    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticBezierTo(cx, cy, cx + r, cy)
        quadraticBezierTo(cx, cy, cx, cy + r)
        quadraticBezierTo(cx, cy, cx - r, cy)
        quadraticBezierTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path, color = color)
}

@Composable
fun RotatingSparkles(modifier: Modifier = Modifier, size: Float = 120f) {
    val infiniteTransition = rememberInfiniteTransition(label = "SparkleRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(size.dp)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2

        rotate(rotation) {
            // 메인 노란색 반짝임
            drawSparkle(Offset(cx, cy), this.size.width * 0.7f, Color(0xFFFFD54F))

            // 서브 작은 반짝임들
            drawSparkle(Offset(cx - this.size.width * 0.25f, cy - this.size.height * 0.25f), this.size.width * 0.25f, Color(0xFFFFE082))
            drawSparkle(Offset(cx + this.size.width * 0.3f, cy + this.size.height * 0.2f), this.size.width * 0.2f, Color(0xFFFFE082))
        }
    }
}

@Composable
fun ThreeDotLoading(modifier: Modifier = Modifier) {
    val dots = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0.0f at 0 with FastOutSlowInEasing
                        -12f at 300 with FastOutSlowInEasing
                        0.0f at 600 with FastOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { animatable ->
            Canvas(modifier = Modifier.size(10.dp).offset(y = animatable.value.dp)) {
                drawCircle(color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// 별 파티클 입자 정의
private data class StarParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    var alpha: Float,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

@Composable
fun StarParticleEffect(modifier: Modifier = Modifier) {
    val particles = remember { mutableStateListOf<StarParticle>() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cx = width / 2
        val cy = height / 2

        // 초기 생성 (사방으로 뿜어져 나가는 35개 파티클)
        if (particles.isEmpty()) {
            val colors = listOf(
                Color(0xFFFFD54F), // 노랑
                Color(0xFFFF8A65), // 오렌지
                Color(0xFFE91E63), // 핑크
                Color(0xFF81C784), // 연두
                Color(0xFF64B5F6)  // 파랑
            )
            for (i in 0 until 35) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = 4f + Random.nextFloat() * 10f
                particles.add(
                    StarParticle(
                        x = cx,
                        y = cy,
                        vx = cos(angle.toDouble()).toFloat() * speed,
                        vy = sin(angle.toDouble()).toFloat() * speed,
                        size = 14f + Random.nextFloat() * 16f,
                        color = colors.random(),
                        alpha = 1f,
                        rotationSpeed = -4f + Random.nextFloat() * 8f
                    )
                )
            }
        }

        // 입자 그리기
        particles.forEach { p ->
            if (p.alpha > 0.02f) {
                rotate(p.rotation, Offset(p.x, p.y)) {
                    drawSparkle(Offset(p.x, p.y), p.size, p.color.copy(alpha = p.alpha))
                }
            }
        }
    }

    // 입자 업데이트 루프 (60fps)
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                particles.forEachIndexed { index, p ->
                    val newX = p.x + p.vx
                    val newY = p.y + p.vy + 0.1f // 중력 약간 적용
                    val newAlpha = p.alpha - 0.02f // 투명도 감소
                    val newRotation = p.rotation + p.rotationSpeed

                    if (newAlpha <= 0f) {
                        particles[index] = p.copy(alpha = 0f)
                    } else {
                        particles[index] = p.copy(
                            x = newX,
                            y = newY,
                            alpha = newAlpha,
                            rotation = newRotation
                        )
                    }
                }
            }
            yield()
        }
    }
}
