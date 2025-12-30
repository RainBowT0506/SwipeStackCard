package com.rainbowt.swipestack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

data class CardItem(val id: Int, val title: String)

enum class SwipeDirection { Left, Right }

@Composable
fun <T> SwipeStack(
    items: List<T>,
    modifier: Modifier = Modifier,
    stackCount: Int = 3,
    showAllCards: Boolean = false,
    onSwiped: (item: T, direction: SwipeDirection) -> Unit = { _, _ -> },
    cardContent: @Composable (item: T) -> Unit
) {
    if (items.isEmpty()) return

    var topIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val threshold = widthPx * 0.25f

        val offsetX = remember(topIndex) { Animatable(0f) }
        val offsetY = remember(topIndex) { Animatable(0f) }

        // 拖曳進度 0..1
        val progress = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)

        // 實際顯示張數：開啟就全顯示，關閉就固定 stackCount
        val visibleCount = if (showAllCards) items.size else stackCount

        // 從後往前畫，最上面最後畫
        val last = (topIndex + visibleCount - 1).coerceAtMost(items.lastIndex)

        // 厚堆疊參數（想更厚就把 dx/dy 再加大）
        val stackDx = 36f        // 每層往右偏移(px)
        val stackDy = 30f        // 每層往下偏移(px)
        val scaleStep = 0.025f   // 每層縮小幅度（不要太大，邊緣才露得出來）
        val liftDx = 44f         // 拖曳時後面往左上頂的幅度(X)
        val liftDy = 52f         // 拖曳時後面往左上頂的幅度(Y)
        val liftScale = 0.04f    // 拖曳時後面放大幅度

        // 後卡視覺切層
        val strokeAlphaStep = 0.08f
        val colorAlphaStep = 0.035f

        for (i in last downTo topIndex) {
            val depth = i - topIndex
            val item = items[i]

            val baseScale = 1f - depth * scaleStep
            val baseTransX = depth * stackDx
            val baseTransY = depth * stackDy

            val transX = if (depth == 0) offsetX.value else baseTransX - liftDx * progress
            val transY = if (depth == 0) offsetY.value else baseTransY - liftDy * progress
            val scale = if (depth == 0) 1f else baseScale + liftScale * progress

            val rotationZ = if (depth == 0) (offsetX.value / widthPx) * 12f else 0f

            // 陰影層次：最上層最厚，越後面越薄
            val elevationDp = (22 - depth * 5).coerceAtLeast(2).dp

            // 後面卡片顏色略深 + 有邊框
            val baseSurface = MaterialTheme.colorScheme.surface
            val cardColor =
                if (depth == 0) baseSurface
                else baseSurface.copy(alpha = (1f - depth * colorAlphaStep).coerceIn(0.85f, 1f))

            val strokeColor =
                Color.Black.copy(alpha = (0.18f - depth * strokeAlphaStep).coerceIn(0.03f, 0.18f))

            Card(
                modifier = Modifier
                    .size(width = 320.dp, height = 200.dp)
                    .zIndex((visibleCount - depth).toFloat()) // 用 visibleCount，避免 showAllCards 時層級怪
                    .graphicsLayer {
                        translationX = transX
                        translationY = transY
                        scaleX = scale
                        scaleY = scale
                        this.rotationZ = rotationZ
                    }
                    .then(
                        if (depth == 0) {
                            Modifier.pointerInput(topIndex) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.15f)
                                        }
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            val x = offsetX.value
                                            val dir =
                                                if (x >= 0) SwipeDirection.Right else SwipeDirection.Left

                                            if (abs(x) > threshold) {
                                                val targetX = sign(x) * (widthPx * 1.2f)
                                                offsetX.animateTo(
                                                    targetX,
                                                    spring(stiffness = Spring.StiffnessMediumLow)
                                                )

                                                onSwiped(item, dir)
                                                topIndex =
                                                    (topIndex + 1).coerceAtMost(items.lastIndex)

                                                offsetX.snapTo(0f)
                                                offsetY.snapTo(0f)
                                            } else {
                                                offsetX.animateTo(
                                                    0f,
                                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                )
                                                offsetY.animateTo(
                                                    0f,
                                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        } else Modifier
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = elevationDp),
                border = if (depth == 0) null else BorderStroke(1.dp, strokeColor),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    cardContent(item)
                }
            }
        }
    }
}