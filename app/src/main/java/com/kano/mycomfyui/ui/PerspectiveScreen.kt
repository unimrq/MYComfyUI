package com.kano.mycomfyui.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kano.mycomfyui.R
import com.kano.mycomfyui.data.FileInfo

enum class ToolMode {
    TRANSFORM, ERASE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerspectiveScreen (
    navController: NavController
){

    val files =
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<List<FileInfo>>("perspective_files")
            ?: emptyList()

    if (files.size != 2) {
        Text("数据异常")
        return
    }

    // 文件名长度排序
    val sorted = files.sortedBy { it.name.length }
    val topFile = sorted[0]     // 文件名短 → 上层
    val bottomFile = sorted[1]  // 文件名长 → 下层

    Box(modifier = Modifier.fillMaxSize()) {
        // 内容
        PerspectiveCompareView(
            topUrl = topFile.net_url,
            bottomUrl = bottomFile.net_url
        )

        // TopBar 覆盖在上面
        TopAppBar(
            title = { Text("透视模式", fontSize = 18.sp) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun PerspectiveCompareView(
    topUrl: String?,
    bottomUrl: String?
) {
    val context = LocalContext.current

    var topBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var bottomBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var restoreTrigger by remember { mutableStateOf(0) }
    var showTopBitmap by remember { mutableStateOf(true) } // 新增隐藏/显示状态
    var cleared by remember { mutableStateOf(false) }

    // 加载上层图片
    LaunchedEffect(topUrl) {
        if (topUrl != null) {
            val loader = ImageLoader(context)
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
            val loader = ImageLoader(context)
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

    if (topBitmap == null || bottomBitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 整体布局：Canvas + 底栏
    Box(Modifier.fillMaxSize()) {
        PerspectiveCanvas(
            topBitmap = topBitmap!!,
            bottomBitmap = bottomBitmap!!,
            restoreTrigger = restoreTrigger,
            showTopBitmap = showTopBitmap
        )

        BottomToolBar(
            onRestoreClicked = {
                restoreTrigger++
                if (cleared) {
                    showTopBitmap = !showTopBitmap
                    cleared = false
                }
            },
            onToggleTopVisibility = {
                showTopBitmap = !showTopBitmap
                cleared = !cleared
            },
            cleared = cleared,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        )
    }
}

@Composable
fun BottomToolBar(
    onRestoreClicked: () -> Unit,
    onToggleTopVisibility: () -> Unit,
    cleared: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color.White)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconActionButton(
            iconVector = Icons.Default.Refresh, // 你的复原图标
            label = "复原",
            tint = Color.Black,
            itemWidth = 60.dp,
            iconSize = 22.dp,

            onClick = { onRestoreClicked() }
        )


        IconActionButton(
            iconPainter = painterResource(id = if(cleared) R.drawable.visibility else R.drawable.visibility_off),
            label = if(cleared) "显示" else "隐藏",
            tint = Color.Black,
            itemWidth = 60.dp,
            iconSize = 22.dp,
            onClick = { onToggleTopVisibility() }
        )
    }
}




@Composable
fun PerspectiveCanvas(
    topBitmap: ImageBitmap,
    bottomBitmap: ImageBitmap,
    restoreTrigger: Int,
    showTopBitmap: Boolean
) {
    val path = remember { Path() }
    var redrawTrigger by remember { mutableStateOf(0) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var baseScale by remember { mutableStateOf(1f) }

    LaunchedEffect(restoreTrigger) {
        scale = 1f
        offset = Offset.Zero
        path.reset()
        redrawTrigger++
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale != 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2f
                            val scaleFactor = targetScale / scale

                            // 重新计算 left, top
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val baseScaleLocal = minOf(
                                canvasWidth / topBitmap.width,
                                canvasHeight / topBitmap.height
                            )
                            val left = (canvasWidth - topBitmap.width * baseScaleLocal) / 2f
                            val top = (canvasHeight - topBitmap.height * baseScaleLocal) / 2f

                            val adjustedCenter = tap - Offset(left, top)

                            offset = offset * scaleFactor + adjustedCenter * (1f - scaleFactor)
                            scale = targetScale
                        }
                        redrawTrigger++
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {

                    val down = awaitFirstDown()
                    var isMultiTouch = false

                    // ---------- 单指开始时立即开启新轨迹 ----------
                    fun convertToImageSpace(touch: Offset): Offset {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        baseScale = minOf(
                            canvasWidth / topBitmap.width.toFloat(),
                            canvasHeight / topBitmap.height.toFloat()
                        )

                        val totalScale = baseScale * scale
                        val left =
                            (canvasWidth - topBitmap.width * baseScale) / 2f
                        val top =
                            (canvasHeight - topBitmap.height * baseScale) / 2f

                        return Offset(
                            (touch.x - left - offset.x) / totalScale,
                            (touch.y - top - offset.y) / totalScale
                        )
                    }

                    var startedErase = false

                    do {
                        val event = awaitPointerEvent()
                        val pointerCount =
                            event.changes.count { it.pressed }

                        // ===== 双指逻辑 =====
                        if (pointerCount >= 2) {
                            isMultiTouch = true

                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()

                            val prevScale = scale
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            val scaleFactor = scale / prevScale

                            // 重新计算 left, top（与绘制时保持一致）
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val baseScaleLocal = minOf(
                                canvasWidth / topBitmap.width,
                                canvasHeight / topBitmap.height
                            )
                            val left = (canvasWidth - topBitmap.width * baseScaleLocal) / 2f
                            val top = (canvasHeight - topBitmap.height * baseScaleLocal) / 2f

                            val adjustedCenter = centroid - Offset(left, top)

                            // 更新 offset：保持 adjustedCenter 对应的图像点不变，再加 pan
                            offset = offset * scaleFactor + adjustedCenter * (1f - scaleFactor) + pan

                            redrawTrigger++
                        }

                        // ===== 单指擦除 =====
                        else if (!isMultiTouch && pointerCount == 1) {

                            val change =
                                event.changes.first()

                            val local =
                                convertToImageSpace(change.position)

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
                }
            }
    ) {
        redrawTrigger

        val canvasWidth = size.width
        val canvasHeight = size.height

        baseScale = minOf(
            canvasWidth / topBitmap.width,
            canvasHeight / topBitmap.height
        )

        val totalScale = baseScale * scale
        val left = (canvasWidth - topBitmap.width * baseScale) / 2f
        val top = (canvasHeight - topBitmap.height * baseScale) / 2f
        val imageSpaceBrush = topBitmap.width * 0.1f
        val bottomWidthScale =
            topBitmap.width.toFloat() / bottomBitmap.width.toFloat()

        // ===== 画底图 =====
        drawContext.canvas.save()
        drawContext.canvas.translate(left + offset.x, top + offset.y)
        drawContext.canvas.scale(totalScale, totalScale)

        drawContext.canvas.save()
        drawContext.canvas.scale(bottomWidthScale, bottomWidthScale)
        drawImage(bottomBitmap)
        drawContext.canvas.restore()

        drawContext.canvas.restore()

        // ===== 画上图 + 擦除 =====
        if (showTopBitmap) {
            drawContext.canvas.saveLayer(size.toRect(), Paint())

            drawContext.canvas.translate(left + offset.x, top + offset.y)
            drawContext.canvas.scale(totalScale, totalScale)

            drawImage(topBitmap)

            drawPath(
                path = path,
                color = Color.Transparent,
                style = Stroke(
                    width = imageSpaceBrush,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Clear
            )

            drawContext.canvas.restore()
        }
    }
}

