package com.rainbowt.swipestackcard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

data class CardItem(val id: Int, val title: String)

@Composable
fun SwipeStack(
    items: List<CardItem>,
    modifier: Modifier = Modifier,
    stackCount: Int = 3,
    onSwiped: (item: CardItem, direction: SwipeDirection) -> Unit = { _, _ -> }
) {
    if (items.isEmpty()) return

    var topIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val threshold = widthPx * 0.25f   // 超過 25% 寬度就算 swipe

        val offsetX = remember(topIndex) { Animatable(0f) } // 換卡就重置 state
        val offsetY = remember(topIndex) { Animatable(0f) }

        // 拖曳進度 0..1，拿來讓後面卡片「跟著浮起來」
        val progress = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)

        // 從後往前畫，最上面最後畫
        val last = (topIndex + stackCount).coerceAtMost(items.lastIndex)

        for (i in last downTo topIndex) {
            val depth = i - topIndex // 0 = 最上層
            val item = items[i]

            val baseScale = 1f - (depth * 0.04f)
            val baseTransY = depth * 14f
            val baseAlpha = 1f - (depth * 0.12f)

            // 後面卡片在你拖曳時稍微往上/變大一點，增加「堆疊活著」的感覺
            val scale = if (depth == 0) 1f else baseScale + (0.02f * progress)
            val transY = if (depth == 0) offsetY.value else baseTransY - (6f * progress)

            val rotationZ = if (depth == 0) {
                // 最大旋轉角度約 12 度
                (offsetX.value / widthPx) * 12f
            } else 0f

            val transX = if (depth == 0) offsetX.value else 0f

            Card(
                modifier = Modifier
                    .size(width = 320.dp, height = 200.dp)
                    .zIndex((stackCount - depth).toFloat())
                    .graphicsLayer {
                        translationX = transX
                        translationY = transY
                        scaleX = scale
                        scaleY = scale
                        alpha = baseAlpha
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
                                            // 想要上下也跟著微動就保留這行；不想上下就拿掉
                                            offsetY.snapTo((offsetY.value + dragAmount.y * 0.15f))
                                        }
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            val x = offsetX.value
                                            val dir = if (x >= 0) SwipeDirection.Right else SwipeDirection.Left

                                            if (abs(x) > threshold) {
                                                // 甩出螢幕
                                                val targetX = sign(x) * (widthPx * 1.2f)
                                                offsetX.animateTo(
                                                    targetX,
                                                    spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                // 切到下一張
                                                onSwiped(item, dir)
                                                topIndex = (topIndex + 1).coerceAtMost(items.lastIndex)

                                                // reset（新卡上來前歸零）
                                                offsetX.snapTo(0f)
                                                offsetY.snapTo(0f)
                                            } else {
                                                // 彈回
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
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Card: ${item.title}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

enum class SwipeDirection { Left, Right }

@Preview
@Composable
fun DemoSwipeStack() {
    val items = remember {
        List(10) { CardItem(it, "Item $it") }
    }

    SwipeStack(
        items = items,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        stackCount = 3,
        onSwiped = { item, dir ->
            // dir = Left / Right
            // 在這裡做你要的處理（例如喜歡/不喜歡、API、更新資料）
        }
    )
}
