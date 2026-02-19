package com.kano.mycomfyui.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kano.mycomfyui.R
import com.kano.mycomfyui.data.FileInfo
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerspectiveScreen(
    files: List<FileInfo>,
    onClose: () -> Unit
) {

    if (files.size != 2) {
        Text("数据异常")
        return
    }

    // 文件名长度排序
    val sorted = files.sortedBy { it.name.length }
    val topFile = sorted[0]
    val bottomFile = sorted[1]

    Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {

        PerspectiveCompareView(
            topUrl = topFile.net_url,
            bottomUrl = bottomFile.net_url,
            onDismiss = onClose
        )
    }
}

@Composable
fun PerspectiveCompareView(
    topUrl: String?,
    bottomUrl: String?,
    onDismiss:() -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { NudePrefs(context) }
    val loader = remember { ImageLoader(context) }
    var topBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var bottomBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var restoreTrigger by remember { mutableStateOf(0) }
    var topAlpha by remember {
        mutableStateOf(
            prefs.get("perspective_topAlpha", "1").toFloat()
        )
    }

    var eraseSize by remember {
        mutableStateOf(
            prefs.get("perspective_eraseSize", "0.12").toFloat()
        )
    }

    var eraseAlpha by remember {
        mutableStateOf(
            prefs.get("perspective_eraseAlpha", "0.64").toFloat()
        )
    }


    var showSizePanel by remember { mutableStateOf(false) }
    var showAlphaPanel by remember { mutableStateOf(false) }
    var showTopAlphaPanel by remember { mutableStateOf(false) }

    val isReady = topBitmap != null && bottomBitmap != null

    var showLoading by remember { mutableStateOf(true) }

    // 加载上层图片
    LaunchedEffect(topUrl) {
        if (topUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(topUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                topBitmap = result.drawable.toBitmap().asImageBitmap()
            }
        }
    }

    // 加载下层图片
    LaunchedEffect(bottomUrl) {
        if (bottomUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(bottomUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                bottomBitmap = result.drawable.toBitmap().asImageBitmap()
            }
        }
    }


    LaunchedEffect(topBitmap, bottomBitmap) {
        if (topBitmap != null && bottomBitmap != null) {
            delay(120) // 延迟 500ms 再关闭
            showLoading = false
        }
    }

    if (!isReady || showLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "请稍后...",
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    } else {
        // 整体布局：Canvas + 底栏
        Box(Modifier.fillMaxSize()) {
            PerspectiveCanvas(
                topBitmap = topBitmap!!,
                bottomBitmap = bottomBitmap!!,
                restoreTrigger = restoreTrigger,
                topAlpha = topAlpha,
                eraseSize = eraseSize,
                eraseAlpha = eraseAlpha
            )

            BottomToolBar(
                onRestoreClicked = {
                    restoreTrigger++
                    topAlpha = 1f
                },
                onToggleSizePanel = {
                    showSizePanel = !showSizePanel
                    showAlphaPanel = false
                    showTopAlphaPanel = false
                },

                onToggleAlphaPanel = {
                    showAlphaPanel = !showAlphaPanel
                    showSizePanel = false
                    showTopAlphaPanel = false
                },

                onToggleTopAlphaPanel = {
                    showTopAlphaPanel = !showTopAlphaPanel
                    showSizePanel = false
                    showAlphaPanel = false
                },
                onClose = {
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
            )

            if (showSizePanel) {
                SingleSliderPanel(
                    value = eraseSize,
                    onValueChange = {
                        eraseSize = it
                        prefs.put("perspective_eraseSize", it.toString())
                    },
                    valueRange = 0.02f..0.3f,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 16.dp)
                        .zIndex(2f)
                )
            }

            if (showAlphaPanel) {
                SingleSliderPanel(
                    value = eraseAlpha,
                    onValueChange = {
                        eraseAlpha = it
                        prefs.put("perspective_eraseAlpha", it.toString())
                    },
                    valueRange = 0.1f..1f,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 16.dp)
                        .zIndex(2f)
                )
            }

            if (showTopAlphaPanel) {
                SingleSliderPanel(
                    value = topAlpha,
                    onValueChange = {
                        topAlpha = it
                        prefs.put("perspective_topAlpha", it.toString())
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 16.dp)
                        .zIndex(2f)
                )
            }

        }
    }

}

@Composable
fun BottomToolBar(
    onRestoreClicked: () -> Unit,
    onToggleTopAlphaPanel: () -> Unit,
    onToggleSizePanel: () -> Unit,
    onToggleAlphaPanel: () -> Unit,
    onClose:() -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .zIndex(3f)
            .background(Color.Black)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconActionButton(
            iconVector = Icons.Default.Clear,
            label = "关闭",
            tint = Color.White,
            itemWidth = 60.dp,
            iconSize = 22.dp,
            onClick = { onClose() }
        )

        IconActionButton(
            iconVector = Icons.Default.Refresh, // 你的复原图标
            label = "复原",
            tint = Color.White,
            itemWidth = 60.dp,
            iconSize = 22.dp,

            onClick = { onRestoreClicked() }
        )

        IconActionButton(
            iconPainter = painterResource(id = R.drawable.opacity_image),
            label = "透明度",
            tint = Color.White,
            itemWidth = 60.dp,
            iconSize = 18.dp,
            onClick = { onToggleTopAlphaPanel() }
        )

        IconActionButton(
            iconPainter = painterResource(id = R.drawable.pen),
            label = "笔刷大小",
            tint = Color.White,
            itemWidth = 60.dp,
            iconSize = 16.dp,
            onClick = { onToggleSizePanel() }
        )

        IconActionButton(
            iconPainter = painterResource(id = R.drawable.opacity),
            label = "笔刷力度",
            tint = Color.White,
            itemWidth = 60.dp,
            iconSize = 20.dp,
            onClick = { onToggleAlphaPanel() }
        )
    }
}


@Composable
fun PerspectiveCanvas(
    topBitmap: ImageBitmap,
    bottomBitmap: ImageBitmap,
    restoreTrigger: Int,
    topAlpha: Float,
    eraseSize: Float,
    eraseAlpha: Float
) {
    val path = remember { Path() }
    var redrawTrigger by remember { mutableStateOf(0) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var baseScale by remember { mutableStateOf(1f) }
    var lastMultiTouchTime by remember { mutableStateOf(0L) }

    LaunchedEffect(restoreTrigger) {
        scale = 1f
        offset = Offset.Zero
        path.reset()
        redrawTrigger++
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)  // ← 直接设置背景
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val now = System.currentTimeMillis()
                        if (now - lastMultiTouchTime < 200) {
                            // 刚做过双指缩放，忽略误触双击
                            return@detectTapGestures
                        }
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // baseScale 保证图片适应 Canvas
                        val baseScale = minOf(
                            canvasWidth / topBitmap.width.toFloat(),
                            canvasHeight / topBitmap.height.toFloat()
                        )

                        // 图片居中偏移
                        val left = (canvasWidth - topBitmap.width * baseScale) / 2f
                        val top = (canvasHeight - topBitmap.height * baseScale) / 2f

                        if (scale != 1f) {
                            // 缩小回原始状态
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2f
                            val clickInImage = (tap - Offset(left, top) - offset)
                            offset = Offset(
                                x = (topBitmap.width / 2f*baseScale - clickInImage.x*targetScale) ,
                                y = (topBitmap.height / 2f*baseScale - clickInImage.y*targetScale)
                            )
                            scale = targetScale
                        }
                        redrawTrigger++
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    var isMultiTouch = false

                    var initialScale = scale
                    var initialOffset = offset
                    var initialDistance = 0f
                    var initialCentroid = Offset.Zero

                    var startedErase = false

                    do {

                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }

                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 图片适应 Canvas 的基础缩放
                        val baseScale = minOf(
                            canvasWidth / topBitmap.width.toFloat(),
                            canvasHeight / topBitmap.height.toFloat()
                        )

                        // 图片居中偏移
                        val left = (canvasWidth - topBitmap.width * baseScale) / 2f
                        val top = (canvasHeight - topBitmap.height * baseScale) / 2f

                        // ===== 双指逻辑 =====
                        if (pointerCount >= 2) {
                            lastMultiTouchTime = System.currentTimeMillis() // 记录双指时间
                            val positions = event.changes.filter { it.pressed }.map { it.position }
                            val centroid = positions.reduce { a, b -> a + b } / positions.size.toFloat()

                            val dx = positions[0].x - positions[1].x
                            val dy = positions[0].y - positions[1].y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (!isMultiTouch) {
                                // 记录初始状态
                                initialDistance = distance
                                initialScale = scale
                                initialOffset = offset
                                initialCentroid = centroid
                                isMultiTouch = true
                            } else if (initialDistance != 0f) {
                                // 缩放比例
                                val zoom = distance / initialDistance
                                val newScale = (initialScale * zoom).coerceIn(0.5f, 3f)

                                // 保持双指中心对应的图片像素点在屏幕上不动
                                val imageCoord = (initialCentroid - Offset(left, top) - initialOffset) / initialScale
                                offset = centroid - imageCoord * newScale - Offset(left, top)

                                scale = newScale
                            }
                        }

                        // ===== 单指擦除 =====
                        else if (!isMultiTouch && pointerCount == 1) {
                            val change = event.changes.first()
                            val local = Offset(
                                x = (change.position.x - left - offset.x) / (baseScale * scale),
                                y = (change.position.y - top - offset.y) / (baseScale * scale)
                            )

                            if (!startedErase) {
                                path.moveTo(local.x, local.y)
                                startedErase = true
                            } else {
                                path.lineTo(local.x, local.y)
                            }

                            change.consume()
                            redrawTrigger++
                        }

                    } while (event.changes.any { it.pressed })
                    isMultiTouch = false
                    startedErase = false
                }
            }
    ) {
        redrawTrigger

        val canvasWidth = size.width
        val canvasHeight = size.height
        val imageSpaceBrush = topBitmap.width * eraseSize

        baseScale = minOf(
            canvasWidth / topBitmap.width,
            canvasHeight / topBitmap.height
        )

        val totalScale = baseScale * scale
        val left = (canvasWidth - topBitmap.width * baseScale) / 2f
        val top = (canvasHeight - topBitmap.height * baseScale) / 2f

// 计算下层图片X/Y缩放，使宽高都对齐上层
        val scaleX = topBitmap.width.toFloat() / bottomBitmap.width
        val scaleY = topBitmap.height.toFloat() / bottomBitmap.height

// ===== 画底图 =====
        drawContext.canvas.save()
        drawContext.canvas.translate(left + offset.x, top + offset.y)
        drawContext.canvas.scale(totalScale, totalScale)

// 下层单独缩放到与上层宽高一致（不保留比例）
        drawContext.canvas.save()
        drawContext.canvas.scale(scaleX, scaleY)
        drawImage(bottomBitmap)
        drawContext.canvas.restore()

        drawContext.canvas.restore()

        // ===== 画上图 + 擦除 =====
        drawContext.canvas.saveLayer(size.toRect(), Paint())

        drawContext.canvas.translate(left + offset.x, top + offset.y)
        drawContext.canvas.scale(totalScale, totalScale)

        drawImage(
            image = topBitmap,
            alpha = topAlpha,
            blendMode = BlendMode.Multiply
        )

        drawPath(
            path = path,
            color = Color.White.copy(alpha = eraseAlpha),
            style = Stroke(
                width = imageSpaceBrush,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = BlendMode.DstOut
        )

        drawContext.canvas.restore()
    }
}

private fun updateValueFromOffset(
    offsetY: Float,
    height: Float,
    paddingPx: Float,
    thumbSizePx: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    if (height <= 0f) return

    val usableHeight = height - paddingPx * 2
    val trackHeight = usableHeight - thumbSizePx

    val adjusted = (offsetY - paddingPx - thumbSizePx / 2f)
        .coerceIn(0f, trackHeight)

    val percent = 1f - (adjusted / trackHeight)

    val newValue =
        valueRange.start +
                percent * (valueRange.endInclusive - valueRange.start)

    onValueChange(newValue)
}

@Composable
fun SingleSliderPanel(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val thumbSize = 14.dp
    val verticalPadding = 12.dp

    var containerHeightPx by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val paddingPx = with(density) { verticalPadding.toPx() }

    Box(
        modifier = modifier
            .width(36.dp)
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(120.dp)
                .background(
                    Color.DarkGray,
                    RoundedCornerShape(16.dp)
                )
                .onSizeChanged {
                    containerHeightPx = it.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            updateValueFromOffset(
                                offset.y,
                                containerHeightPx,
                                paddingPx,
                                thumbSizePx,
                                valueRange,
                                onValueChange
                            )
                        },
                        onVerticalDrag = { change, _ ->
                            updateValueFromOffset(
                                change.position.y,
                                containerHeightPx,
                                paddingPx,
                                thumbSizePx,
                                valueRange,
                                onValueChange
                            )
                            change.consume()
                        }
                    )
                }
        ) {
            if (containerHeightPx <= 0f) return@Box
            val usableHeight = containerHeightPx - paddingPx * 2
            val trackHeight = usableHeight - thumbSizePx

            val percent =
                ((value - valueRange.start) /
                        (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)

// ===== 轨道 =====
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(vertical = verticalPadding)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.Center)
            )

// ===== thumb（从底部算）=====
            val thumbOffsetPx =
                paddingPx + trackHeight * (1f - percent)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                    .size(thumbSize)
                    .background(Color.White, CircleShape)
            )

// ===== 进度条（从底部到 thumb 中心）=====
            val progressHeightPx =
                percent * trackHeight + thumbSizePx

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(with(density) { progressHeightPx.toDp() })
                    .align(Alignment.BottomCenter)
                    .padding(bottom = verticalPadding)
                    .background(
                        Color.White,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}