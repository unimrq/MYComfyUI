package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt


@OptIn(UnstableApi::class)
@SuppressLint("UnrememberedMutableState", "ConfigurationScreenWidthHeight")
@Composable
fun ImageDetailScreen(
    imagePaths: MutableList<String>,
    filePaths: MutableList<String>,
    thumbPaths: MutableList<String>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onImageClick: () -> Unit,
    onGenerateClick: ((String) -> Unit)? = null,
    onSelectedFileChange: ((filePath: String) -> Unit)? = null,
    visibleFileCoordsMap: MutableState<MutableMap<String, ImageBounds>>,
    onScrollToPosition: (Int) -> Unit

) {
    Log.d("ImageDetailScreen", "visibleFileCoordsMap: $visibleFileCoordsMap")

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var isTopBarVisible by remember { mutableStateOf(true) }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    if (imagePaths.isEmpty()) return
    val imagePath = imagePaths[currentIndex]
    val isVideo = imagePath.lowercase().endsWith(".mp4")

    // 缩放和平移
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f
    val doubleTapScale = 3f

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var currentImageBounds by remember { mutableStateOf<ImageBounds?>(null) }

    val fullImageRect = remember(imageSize) {
        if (imageSize.width == 0 || imageSize.height == 0) {
            null
        } else {
            calculateFitRect(
                imageSize = imageSize,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx
            )
        }
    }
    val animOffsetX = remember { Animatable(0f) } // X 坐标动画
    val animOffsetY = remember { Animatable(0f) } // Y 坐标动画
    val animWidth = remember { Animatable(0f) } // 宽度动画
    val animHeight = remember { Animatable(0f) } // 高度动画


    // 临时存储单指滑动的累计距离
    var dragAccumulation by remember { mutableStateOf(Offset.Zero) }

    // 动画
    // 下拽相关
    var dragY by remember { mutableStateOf(0f) }
    var isDraggingDown by remember { mutableStateOf(false) }
    var dragYAddAble by remember { mutableStateOf(true) }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // 阈值
    val animateThreshold = screenHeightPx * 0.5f
    val closeThreshold = screenHeightPx * 0.08f


//    val dragProgress = (dragY / closeThreshold).coerceIn(0f, 1f)
//
//    val startRect = thumbBounds?.toRect() ?: Rect(0f, 0f, screenWidthPx, screenHeightPx)
//
//    val endRect = remember(thumbBounds) {
//        thumbBounds?.toRect()
//    }
    val dismissProgress = remember { Animatable(0f) }
//    fun lerpRect(start: Rect, end: Rect, fraction: Float): Rect {
//        return Rect(
//            lerp(start.left, end.left, fraction),
//            lerp(start.top, end.top, fraction),
//            lerp(start.right, end.right, fraction),
//            lerp(start.bottom, end.bottom, fraction)
//        )
//    }
//    val cornerRadius = lerp(start = 0.dp, stop = 16.dp, fraction = dismissProgress.value)

    var isAnimateTriggered by remember { mutableStateOf(false) }
    var isCloseTriggered by remember { mutableStateOf(false) }
    val dragScale = if (!isAnimateTriggered) {
        val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
        androidx.compose.ui.util.lerp(
            start = 1f,
            stop = 0.3f,
            fraction = dragProgress
        )
    } else {
        0.3f // 达到关闭阈值后固定，不缩放
    }

    val backgroundAlpha = if (!isAnimateTriggered) {
        val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
        // 非线性快速衰减
        1f - dragProgress.pow(0.04f)
    } else {
        0f
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if(isTopBarVisible) Color.White.copy(alpha = backgroundAlpha) else Color.Black.copy(alpha = backgroundAlpha))
            .onSizeChanged { containerSize = it }
            .zIndex(if(isTopBarVisible) 0f else 12f)
            .pointerInput(currentIndex) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // 计算新的缩放比例，限制在最小和最大比例之间
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                    if (!isAnimateTriggered) {
                        scale = newScale

                        // 如果当前缩放比例大于 1，则进入放大状态，平移图片
                        if (scale > 1f) {
                            offset += pan
                        } else if (zoom == 1f && scale == 1f) {
                            // 在原始比例下处理竖直拖动
                            if (pan.y > 0) {
                                // 下拉触发
                                isDraggingDown = true
                                if (dragYAddAble) {
                                    dragY += pan.y
                                }
                                offset += pan / newScale

                                // 达到关闭阈值时触发关闭
                                if (dragY > animateThreshold) {
                                    isAnimateTriggered = true
                                }
                                if (dragY > closeThreshold) {
                                    isCloseTriggered = true
                                }
                            } else if (pan.y < 0) {
                                // 上拉恢复默认位置或触发关闭动画
                                if (isCloseTriggered) {
                                    isAnimateTriggered = false
                                    offset += pan / newScale
                                    dragYAddAble = false
                                } else {
                                    offset += pan / newScale
                                    scope.launch {
                                        offsetAnim.snapTo(offset)
                                        offsetAnim.animateTo(Offset.Zero, animationSpec = tween(200, easing = FastOutSlowInEasing)) {
                                            offset = value
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 如果动画触发，继续平移
                        offset += pan / newScale
                    }
                }
            }
            .pointerInput(Unit) {
                // 双击放大/还原
                detectTapGestures(
                    onTap = {
                        if (!isVideo) {
                            onImageClick()  // 图像点击处理
                        }
                        isTopBarVisible = !isTopBarVisible  // 显示/隐藏顶部工具栏
                    },
                    onDoubleTap = {
                        // 双击放大或还原
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = doubleTapScale
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // 检测是否所有触摸事件已结束
                        if (event.changes.all { !it.pressed }) {
                            if (isCloseTriggered) {
                                // 播放关闭动画并触发关闭
                                scope.launch {
                                    dismissProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                                    )
                                    onClose()  // 关闭图像查看器
                                }
//                                scope.launch {
//                                    // 飞向相册的平移和缩放动画
//                                    animOffsetX.animateTo(
//                                        targetValue = currentImageBounds?.left ?: 0f,
//                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                                    )
//                                    animOffsetY.animateTo(
//                                        targetValue = currentImageBounds?.top ?: 0f,
//                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                                    )
//                                    animWidth.animateTo(
//                                        targetValue = currentImageBounds?.width ?: 0f,
//                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                                    )
//                                    animHeight.animateTo(
//                                        targetValue = currentImageBounds?.height ?: 0f,
//                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                                    )
//
//                                    // 等待飞向相册的动画完成后，再进行关闭动画
//                                    delay(500) // 等待飞向相册的动画完成
//
//                                    // 执行关闭透明度动画
//                                    dismissProgress.animateTo(
//                                        targetValue = 1f,
//                                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
//                                    )
//
//                                    // 最后触发关闭
//                                    onClose()  // 关闭图像查看器
//                                }
                            } else {
                                // 回弹至原始位置
                                if (scale == 1f) {
                                    dragY = 0f
                                    offset = Offset.Zero
                                    isDraggingDown = false
                                }
                            }
                            isAnimateTriggered = false
                        }
                    }
                }
            }



    ) {
//        fullImageRect?.let { rect ->
//            Box(
//                modifier = Modifier
//                    .offset {
//                        IntOffset(
//                            rect.left.roundToInt(),
//                            rect.top.roundToInt()
//                        )
//                    }
//                    .size(
//                        with(LocalDensity.current) { rect.width.toDp() },
//                        with(LocalDensity.current) { rect.height.toDp() }
//                    )
//                    .border(5.dp, Color.Red)
//            )
//        }


        if (isVideo) {
            Box(modifier = Modifier.fillMaxSize()) {
                CachedVideoPlayer(
                    imagePath,
                    secretKey = ServerConfig.secret,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        else {
            val pagerState = rememberPagerState(initialPage = currentIndex, pageCount = { imagePaths.size })
            val userScrollEnabled = scale == 1f

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp,
                userScrollEnabled = userScrollEnabled
            ) { page ->
                val imagePath = imagePaths[page]
                val thumbPath = thumbPaths.getOrNull(page)

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .collect { page ->
                            if (page != currentIndex) {
                                currentIndex = page
                                onSelectedFileChange?.invoke(imagePaths[currentIndex])
                            }

//                            dragYAddAble = true
//                            isAnimateTriggered = false
//                            isCloseTriggered = false
//                            isDraggingDown = false
                        }

                }



                LaunchedEffect(currentIndex) {
//                    delay(50)
                    // 从可见区域的 map 中获取当前图片的位置
                    val path = filePaths[currentIndex]
                    currentImageBounds = visibleFileCoordsMap.value[path]
//                    Log.d("ImageDetailScreen", "Current Path: $path")
//                    Log.d("ImageDetailScreen", "Current Image Bounds: $currentImageBounds")
                    // 如果目标图片不在当前可见区域，滚动到目标图片
                    onScrollToPosition(currentIndex)
                }

//                LaunchedEffect(currentIndex) {
//                    currentImageBounds?.let { bounds ->
//                        // 从相册位置动画到大图位置
//                        animOffsetX.animateTo(
//                            targetValue = bounds.left,
//                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                        )
//                        animOffsetY.animateTo(
//                            targetValue = bounds.top,
//                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                        )
//
//                        // 从相册大小动画到大图大小
//                        animWidth.animateTo(
//                            targetValue = bounds.width,
//                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                        )
//                        animHeight.animateTo(
//                            targetValue = bounds.height,
//                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
//                        )
//                    }
//                }



//                Box(
//                    modifier = Modifier
//                        .graphicsLayer(
//                            scaleX = animWidth.value / 100f,  // 简单的 100f 宽度作为基准
//                            scaleY = animHeight.value / 100f,  // 简单的 100f 高度作为基准
//                            translationX = animOffsetX.value,
//                            translationY = animOffsetY.value
//                        )
//                        .size(100.dp)  // 初始尺寸
//                        .background(Color.Blue)
//                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
//                    Log.d("Debug", "animOffsetX: ${animOffsetX.value}, animOffsetY: ${animOffsetY.value}")
//                    Log.d("Debug", "animWidth: ${animWidth.value}, animHeight: ${animHeight.value}")

                    val isGif = imagePath.lowercase().endsWith(".gif")
                    if (isGif){
                        ImageRequest.Builder(context)
                            .data(imagePath)
                            .size(Size.ORIGINAL)
                            .apply {
                                decoderFactory(ImageDecoderDecoder.Factory())
                            }
                            .build()

                    } else {
//                        currentImageBounds?.let { bounds ->
//                            Canvas(
//                                modifier = Modifier.fillMaxSize()  // 设置透明度为0，完全透明
//
//                            ) {
//                                drawRect(
//                                    color = Color.Red.copy(alpha = 0.3f), // 红色矩形，透明度0.3
//                                    topLeft = Offset(bounds.left, bounds.top),  // 矩形的左上角位置
//                                    size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height)   // 矩形的宽度和高度
//                                )
//                            }
//                        }
//                        currentImageBounds?.let {
//                            Canvas(modifier = Modifier.fillMaxSize()) {
//                                drawRect(
//                                    color = Color.Red.copy(alpha = 0.3f),
//                                    topLeft = Offset(animOffsetX.value, animOffsetY.value),
//                                    size = androidx.compose.ui.geometry.Size(animWidth.value, animHeight.value)
//                                )
//                            }
//                        }
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagePath)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .size(coil.size.Size(screenWidthPx.roundToInt(), screenHeightPx.roundToInt()))
                                .listener(
                                    onSuccess = { _, result ->
                                        imageSize = IntSize(
                                            result.drawable.intrinsicWidth,
                                            result.drawable.intrinsicHeight
                                        )
                                    }
                                )

                                .apply {
                                    if (imagePath.lowercase().endsWith(".gif")) {
                                        decoderFactory(ImageDecoderDecoder.Factory())
                                    }
                                }
                                .build(),
//                    imageLoader = imageLoader,
                            contentDescription = "大图",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .onSizeChanged { if (imageSize == IntSize.Zero) imageSize = it }
                                .graphicsLayer(
                                    scaleX = scale * dragScale,
                                    scaleY = scale * dragScale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    alpha = 1f - dismissProgress.value  // alpha 从 1 → 0
                                )
                                .fillMaxSize(),
                            loading = {
//                                val thumbPath = thumbPaths.getOrNull(currentIndex)
                                // ✅ 显示缩略图作为占位
                                if (thumbPath != null) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbPath)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .size(Size.ORIGINAL)
                                            .build(),
//                                imageLoader = imageLoader,
                                        contentDescription = "缩略图",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // 缩略图不存在时显示加载指示器
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White)
                                    }
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("加载失败", color = Color.Red)
                                }
                            },
                            success = {
                                SubcomposeAsyncImageContent()  // 原图加载完毕后显示
                            }
                        )
                    }
                }
            }
            // ✅ 页码固定在屏幕底部中央
            if(!isTopBarVisible){
                Text(
                    text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }

        }
    }
}
