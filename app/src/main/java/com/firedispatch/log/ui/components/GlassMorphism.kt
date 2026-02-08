package com.firedispatch.log.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * リキッドグラススタイルのカードコンポーネント（プロダクション品質）
 * ガラスのような半透明で滑らかな質感を提供
 *
 * 設計方針:
 * - blur()はコンテンツをぼかすため使用しない
 * - 下部を濃くして安定感を出す（垂直グラデーション）
 * - 下エッジに線を引いて境界を明確に
 * - 上部40%にハイライトで光沢感
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    baseAlpha: Float = 0.45f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            // 垂直グラデーション: 上は薄く、下はかなり濃く（重心を下に）
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = baseAlpha - 0.15f),  // 上: より薄く
                        Color.White.copy(alpha = baseAlpha + 0.15f)   // 下: より濃く
                    )
                )
            )
            // 境界線（シャープさを出す）
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(radius)
            )
            .graphicsLayer {
                shadowElevation = 10.dp.toPx()  // 影を減らす
                shape = RoundedCornerShape(radius)
            }
            .drawBehind {
                // 下エッジ強調線（境界を明確に）
                val stroke = 1.5.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, size.height - stroke),
                    end = Offset(size.width, size.height - stroke),
                    strokeWidth = stroke
                )
            }
    ) {
        // 上部ハイライト（光沢感、上40%のみ）
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.4f
                        )
                    )
                }
        )
        content()
    }
}

/**
 * リキッドグラススタイルの背景コンポーネント
 * グラデーションと流動的なアニメーション
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquidGlass")

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .background(
                brush = if (animated) {
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.7f),
                            secondaryColor.copy(alpha = 0.6f),
                            tertiaryColor.copy(alpha = 0.5f),
                            primaryColor.copy(alpha = 0.3f)  // 画面下も色を残す
                        ),
                        center = Offset(animatedOffset, animatedOffset),
                        radius = 1500f
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.6f),
                            secondaryColor.copy(alpha = 0.5f),
                            tertiaryColor.copy(alpha = 0.4f)
                        )
                    )
                }
            )
    ) {
        content()
    }
}
