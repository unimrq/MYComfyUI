package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kano.mycomfyui.MyApp
import kotlin.math.abs
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.size.Size
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.zIndex
import kotlin.math.pow

@OptIn(UnstableApi::class)
@SuppressLint("RememberReturnType")
@Composable
fun CachedVideoPlayer(
    videoPath: String,
    secretKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current


    val exoPlayer = remember {
        // 1️⃣ OkHttpClient 带 X-Secret Header
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                // 仅网络 URL 才加 Header
                if (videoPath.startsWith("http")) {
                    requestBuilder.addHeader("X-Secret", secretKey)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        // 2️⃣ OkHttpDataSource.Factory
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        // 3️⃣ CacheDataSource.Factory
        val cacheFactory = CacheDataSource.Factory()
            .setCache(MyApp.simpleCache) // 你的缓存对象
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(MyApp.simpleCache))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // 4️⃣ 创建 ExoPlayer
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(videoPath))
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
    }

    // 自动释放
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Compose UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}


@OptIn(UnstableApi::class)
@SuppressLint("UnrememberedMutableState", "ConfigurationScreenWidthHeight")
@Composable
fun ImageDetailScreen(
    imagePaths: MutableList<String>,
    filePaths: MutableList<String>,
    thumbPaths: MutableList<String>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onImageClick:() -> Unit,
    onGenerateClick: ((String) -> Unit)? = null,
    onSelectedFileChange: ((filePath: String) -> Unit)? = null,
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var isTopBarVisible by remember { mutableStateOf(true) }
    var showControlBar by remember { mutableStateOf(true) }

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

//    val fullImageRect = remember(imageSize) {
//        if (imageSize.width == 0 || imageSize.height == 0) {
//            null
//        } else {
//            calculateFitRect(
//                imageSize = imageSize,
//                screenWidth = screenWidthPx,
//                screenHeight = screenHeightPx
//            )
//        }
//    }

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
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)

//                    Log.d("ges", isDismissTriggered.toString())
                    if (!isAnimateTriggered) {
                        // ✅ 未达到关闭阈值
//                        offset = (offset + centroid) - ((centroid) * (newScale / scale))
                        scale = newScale

                        if (scale > 1f) {
                            // 放大状态 → 平移图片
//                            val newOffset = offset + pan
//                            val scaledWidth = imageSize.width * scale
//                            val scaledHeight = imageSize.height * scale
//                            val maxOffsetX = maxOf((scaledWidth - containerSize.width) / 2f, 0f)
//                            val maxOffsetY = maxOf((scaledHeight - containerSize.height) / 2f, 0f)
//                            offset = Offset(
//                                newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
//                                newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
//                            )
                            offset += pan
                        } else if (zoom == 1f && scale == 1f) {

                            // 原始比例下
                            if (pan.y > 0) {
                                // 下拉
                                isDraggingDown = true
                                if (dragYAddAble){
                                    dragY += pan.y
                                }
                                offset += pan / newScale
                                if (dragY > animateThreshold) {
                                    isAnimateTriggered = true
                                }
                                if (dragY > closeThreshold) {
                                    isCloseTriggered = true
                                }
                            } else if (pan.y < 0){
                                if(isCloseTriggered){
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
//                            else if (!isDraggingDown) {
//                                // 左右滑切图
//                                dragAccumulation += pan
//                                if (abs(dragAccumulation.x) > 120f) {
//                                    if (dragAccumulation.x > 0 && currentIndex > 0) currentIndex--
//                                    else if (dragAccumulation.x < 0 && currentIndex < imagePaths.lastIndex) currentIndex++
//                                    dragAccumulation = Offset.Zero
////                                    onSelectedFileChange?.invoke(imagePaths[currentIndex])
//                                }
//                            }
                        }
                    } else {
                        offset += pan / newScale
//                        dragY += pan.y
                    }
                }
            }
            .pointerInput(Unit) {
                // ✅ 双击放大/还原
                detectTapGestures(
                    onTap = {
//                        if(!isVideo){
//                            onGenerateClick?.invoke(imagePaths[currentIndex])
//                        }
                        if(!isVideo){
                            onImageClick()

                        }
                        isTopBarVisible = !isTopBarVisible
                    },
                    onDoubleTap = {
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
                        if (event.changes.all { !it.pressed }) {
                            if (isCloseTriggered) {
                                // 播放关闭动画
//                                scope.launch {
//                                    dismissProgress.snapTo(0f)
//                                    dismissProgress.animateTo(
//                                        targetValue = 1f,
//                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
//                                    )
//                                    onClose()
//                                }
                                scope.launch {
                                    dismissProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 120, // 时间适当拉长
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onClose()
                                }

                            } else {
                                // 回弹
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
//                    .border(2.dp, Color.Red)
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

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
