package com.kano.mycomfyui.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kano.mycomfyui.data.FileInfo
import kotlin.io.path.Path
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult


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
    val sorted = files.sortedBy { it.name?.length ?: 0 }

    val topFile = sorted[0]     // 文件名短 → 上层
    val bottomFile = sorted[1]  // 文件名长 → 下层

    PerspectiveCompareView(
        topUrl = topFile.net_url,
        bottomUrl = bottomFile.net_url
    )
}

@Composable
fun PerspectiveCompareView(
    topUrl: String?,
    bottomUrl: String?
) {
    val context = LocalContext.current

    var topBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(topUrl) {
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

    if (topBitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PerspectiveCanvas(
        topBitmap = topBitmap!!,
        bottomUrl = bottomUrl
    )
}

@Composable
fun PerspectiveCanvas(
    topBitmap: ImageBitmap,
    bottomUrl: String?
) {
    val path = remember { Path() }

    // ⭐ 关键：用于触发重绘
    var redrawTrigger by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {

        // 底层
        AsyncImage(
            model = bottomUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            path.moveTo(it.x, it.y)
                            redrawTrigger++   // ⭐ 触发刷新
                        },
                        onDrag = { change, _ ->
                            path.lineTo(
                                change.position.x,
                                change.position.y
                            )
                            redrawTrigger++   // ⭐ 触发刷新
                        }
                    )
                }
        ) {

            // ⭐ 关键：读取 redrawTrigger 让 Compose 监听它
            redrawTrigger

            drawContext.canvas.saveLayer(size.toRect(), Paint())

            val imageWidth = topBitmap.width.toFloat()
            val imageHeight = topBitmap.height.toFloat()

            val canvasWidth = size.width
            val canvasHeight = size.height

            val scale = minOf(
                canvasWidth / imageWidth,
                canvasHeight / imageHeight
            )

            val scaledWidth = imageWidth * scale
            val scaledHeight = imageHeight * scale

            val left = (canvasWidth - scaledWidth) / 2f
            val top = (canvasHeight - scaledHeight) / 2f

            drawImage(
                image = topBitmap,
                dstOffset = IntOffset(left.toInt(), top.toInt()),
                dstSize = IntSize(
                    scaledWidth.toInt(),
                    scaledHeight.toInt()
                )
            )

            drawPath(
                path = path,
                color = Color.Transparent,
                style = Stroke(
                    width = 120f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Clear
            )

            drawContext.canvas.restore()

        }
    }
}