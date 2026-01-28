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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.Log
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
@SuppressLint("UnrememberedMutableState", "ConfigurationScreenWidthHeight",
    "UnnecessaryComposedModifier"
)
@Composable
fun ImageDetailScreen(
    imagePaths: MutableList<String>,
    filePaths: MutableList<String>,
    thumbPaths: MutableList<String>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onImageClick: () -> Unit,
    onGenerateClick: ((String) -> Unit)? = null,
    onSelectedFileChange: ((String) -> Unit)? = null,
    visibleCoordsMap: SnapshotStateMap<String, LayoutCoordinates> ,  // ✅ 新增

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
    var isClosing by remember { mutableStateOf(false) }

    var isAnimateTriggered by remember { mutableStateOf(false) }
    var isCloseTriggered by remember { mutableStateOf(false) }
    val dragScale by derivedStateOf {
        if (!isAnimateTriggered) {
            val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
            androidx.compose.ui.util.lerp(start = 1f, stop = 0.3f, fraction = dragProgress)
        } else {
            0.3f
        }
    }

    val backgroundAlpha = if (!isAnimateTriggered) {
        val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
        // 非线性快速衰减
        1f - dragProgress.pow(0.04f)
    } else {
        0f
    }

    var lockedRect by remember { mutableStateOf<Rect?>(null) }
    var startImageRect by remember { mutableStateOf<Rect?>(null) }

    startImageRect = calculateFitRect(
        imageSize = imageSize,
        screenWidth = screenWidthPx,
        screenHeight = screenHeightPx,
        scale = 1f,
        offset = Offset.Zero
    )

    val fullImageRect by remember {
        derivedStateOf {
            if (imageSize.width == 0 || imageSize.height == 0) null
            else {
                // 如果已经触发关闭，返回锁定的值
                if (isAnimateTriggered) {
                    lockedRect ?: calculateFitRect(
                        imageSize = imageSize,
                        screenWidth = screenWidthPx,
                        screenHeight = screenHeightPx,
                        scale = scale * dragScale,
                        offset = offset
                    ).also { lockedRect = it }
                } else {
                    calculateFitRect(
                        imageSize = imageSize,
                        screenWidth = screenWidthPx,
                        screenHeight = screenHeightPx,
                        scale = scale * dragScale,
                        offset = offset
                    )
                }
            }
        }
    }


    val animateFraction = remember { Animatable(0f) }
    val coords = visibleCoordsMap[filePaths[currentIndex]]
    val thumbRect = coords?.let {
        val position = it.positionInWindow()   // 左上角相对于窗口
        val size = it.size                     // IntSize
        Rect(
            left = position.x,
            top = position.y,
            right = position.x + size.width,
            bottom = position.y + size.height
        )
    }

    val startRectNonNull = fullImageRect ?: Rect(0f, 0f, screenWidthPx, screenHeightPx)
    val endRectNonNull = thumbRect ?: startRectNonNull
//    val animatedRect = lerpRect(startRectNonNull, endRectNonNull, animateFraction.value)
//    val scaleOffset = lerpScaleOffset(startRectNonNull, endRectNonNull, animateFraction.value)

    data class Transform(
        val scale: Float,
        val offset: Offset
    )



    data class CloseAnimSnapshot(
        val startRect: Rect,
        val endRect: Rect
    )



    var closeSnapshot by remember { mutableStateOf<CloseAnimSnapshot?>(null) }

//    val targetTransform: Transform? = closeSnapshot?.let { snapshot ->
//
//        val scaleX = snapshot.endRect.width / snapshot.startRect.width
//        val scaleY = snapshot.endRect.height / snapshot.startRect.height
//
//        // 因为你是“先裁成正方形再飞”，这里用 max 是对的
//        val targetScale = maxOf(scaleX, scaleY)
//
//        val startCenter = snapshot.startRect.center
//        val endCenter = snapshot.endRect.center
//
//        // ⭐ 关键修正点
//        val targetOffset = endCenter - startCenter * targetScale
//
//        Transform(
//            scale = targetScale,
//            offset = targetOffset
//        )
//    }

    fun rectToRectTransform(
        start: Rect,
        end: Rect,
    ): Transform {

        val scaleX = end.width / start.width
        val scaleY = end.height / start.height

        val scale = maxOf(scaleX, scaleY)
        val startCenter = start.center
        val endCenter = end.center

        // ⭐ 关键修正点
        val targetOffset = endCenter - startCenter

        return Transform(
            scale = scale,
            offset = targetOffset
        )
    }

    val start = startRectNonNull
    val end = endRectNonNull

    val targetTransform = rectToRectTransform(
        start = startImageRect!!,
        end = end,
    )

    fun contentFitRect(
        container: Rect,
        imageSize: IntSize
    ): Rect {
        val containerRatio = container.width / container.height
        val imageRatio = imageSize.width.toFloat() / imageSize.height

        return if (imageRatio > containerRatio) {
            // 图片更宽 → 上下留空
            val height = container.width / imageRatio
            val top = (container.height - height) / 2f
            Rect(0f, top, container.width, top + height)
        } else {
            // 图片更高 → 左右留空
            val width = container.height * imageRatio
            val left = (container.width - width) / 2f
            Rect(left, 0f, left + width, container.height)
        }
    }

    fun calculateImageDisplayRect(): Rect? {
        if (imageSize.width == 0 || imageSize.height == 0) return null

        val imageRatio = imageSize.width.toFloat() / imageSize.height.toFloat()
        val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

        return if (imageRatio > containerRatio) {
            // 图片更宽 → 横向填满，上下留空
            val scaledHeight = containerSize.width.toFloat() / imageRatio
            val top = (containerSize.height.toFloat() - scaledHeight) / 2f
            Rect(0f, top, containerSize.width.toFloat(), top + scaledHeight)
        } else {
            // 图片更高 → 纵向填满，左右留空
            val scaledWidth = containerSize.height.toFloat() * imageRatio
            val left = (containerSize.width.toFloat() - scaledWidth) / 2f
            Rect(left, 0f, left + scaledWidth, containerSize.height.toFloat())
        }
    }

    fun centerCropRect(src: Rect, targetRatio: Float): Rect {
        val srcRatio = src.width / src.height
        return if (srcRatio > targetRatio) {
            // 横向过宽 → 裁左右
            val newWidth = src.height * targetRatio
            val dx = (src.width - newWidth) / 2f
            Rect(dx, 0f, dx + newWidth, src.height)
        } else {
            // 纵向过高 → 裁上下
            val newHeight = src.width / targetRatio
            val dy = (src.height - newHeight) / 2f
            Rect(0f, dy, src.width, dy + newHeight)
        }
    }

    fun lerpTransform(
        start: Transform,
        end: Transform,
        fraction: Float
    ): Transform {
        return Transform(
            scale = androidx.compose.ui.util.lerp(start.scale, end.scale, fraction),
            offset = Offset(
                x = androidx.compose.ui.util.lerp(start.offset.x, end.offset.x, fraction),
                y = androidx.compose.ui.util.lerp(start.offset.y, end.offset.y, fraction)
            )
        )
    }

    val startClipRect = fullImageRect?.let {
        contentFitRect(
            container = Rect(0f, 0f, it.width, it.height),
            imageSize = imageSize
        )
    }


    val targetClipRect = startClipRect?.let {
        centerCropRect(it, targetRatio = 1f)
    }


    fun triggerClose() {
        val full = fullImageRect
        val thumb = thumbRect
        if (full != null && thumb != null &&
            startClipRect != null && targetClipRect != null
        ) {
            if (closeSnapshot == null) {
                closeSnapshot = CloseAnimSnapshot(
                    startRect = full,
                    endRect = thumb,
                )
            }

            isAnimateTriggered = true

        }

    }

    @SuppressLint("RestrictedApi")
    fun lerpRect(a: Rect, b: Rect, t: Float): Rect =
        Rect(
            lerp(a.left, b.left, t),
            lerp(a.top, b.top, t),
            lerp(a.right, b.right, t),
            lerp(a.bottom, b.bottom, t)
        )

//    val animatedClipRect =
//        closeSnapshot?.let {
//            lerpRect(it.startClip, it.endClip, animateFraction.value)
//        }




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
                                if (dragY > animateThreshold && closeSnapshot == null) {
                                    triggerClose()
                                }
                                if (dragY > closeThreshold) {
                                    isCloseTriggered = true
                                }
//                                if (dragY > closeThreshold) {
//
//
//                                }

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
//                        closeSnapshot = CloseAnimSnapshot(
//                            startRect = fullImageRect!!,
//                            endRect = thumbRect!!
//                        )
//                        isClosing = true
//                        scope.launch {
//                            animateFraction.snapTo(0f)
//                            animateFraction.animateTo(
//                                targetValue = 1f,
//                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
//                            )
//                            // 动画完成后执行关闭
//                            onClose()
//                        }
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
//                                scope.launch {
//                                    dismissProgress.animateTo(
//                                        targetValue = 1f,
//                                        animationSpec = tween(
//                                            durationMillis = 120, // 时间适当拉长
//                                            easing = FastOutSlowInEasing
//                                        )
//                                    )
//                                    onClose()
//                                }
//                                closeSnapshot = CloseAnimSnapshot(
//                                    startRect = fullImageRect!!,
//                                    endRect = thumbRect!!
//                                )
                                isClosing = true   // ⭐ 真正的动画开始

                                scope.launch {
                                    animateFraction.snapTo(0f)
                                    animateFraction.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                    )
                                    // 动画完成后执行关闭
                                    onClose()
                                }
//                                    onClose()

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
        val currentTransform = Transform(
            scale = scale * dragScale,
            offset = offset
        )
        val renderTransform =
            if (isClosing) {
//                Log.d(
//                    "Transform",
//                    "current = scale=${currentTransform.scale}, offset=${currentTransform.offset}"
//                )
//                Log.d(
//                    "Transform",
//                    "target  = scale=${targetTransform.scale}, offset=${targetTransform.offset}"
//                )
//                Log.d(
//                    "Transform",
//                    "fraction=${animateFraction.value}"
//                )

                lerpTransform(currentTransform, targetTransform, animateFraction.value)
            } else {
                currentTransform
            }


//        fullImageRect?.let { rect ->
//            val density = LocalDensity.current
//            val expandDp = 2.dp
//
//            Box(
//                modifier = Modifier
//                    .offset {
//                        IntOffset(
//                            (rect.left - with(density) { expandDp.toPx() }).roundToInt(),
//                            (rect.top - with(density) { expandDp.toPx() }).roundToInt()
//                        )
//                    }
//                    .size(
//                        with(density) { (rect.width + 2 * expandDp.toPx()).toDp() },
//                        with(density) { (rect.height + 2 * expandDp.toPx()).toDp() }
//                    )
//                    .border(expandDp, Color.Red)
//            )
//        }
//
//        thumbRect?.let { rect ->
//            val density = LocalDensity.current
//            val borderWidth = 2.dp
//
//            Box(
//                modifier = Modifier
//                    .offset {
//                        IntOffset(
//                            rect.left.roundToInt(),
//                            rect.top.roundToInt()
//                        )
//                    }
//                    .size(
//                        with(density) { rect.width.toDp() },
//                        with(density) { rect.height.toDp() }
//                    )
//                    .border(borderWidth, Color.Green) // 用绿色标识缩略图
//            )
//        }
//        closeSnapshot?.let { snap ->
//            Canvas(modifier = Modifier.fillMaxSize().zIndex(999f)) {
//                drawCircle(
//                    color = Color.Red,
//                    radius = 8.dp.toPx(),
//                    center = snap.startRect.center
//                )
//
//                drawCircle(
//                    color = Color.Blue,
//                    radius = 8.dp.toPx(),
//                    center = snap.endRect.center
//                )
//            }
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


//                val density = LocalDensity.current
//                val offsetX = with(density) { animatedRect.left.toDp() }
//                val offsetY = with(density) { animatedRect.top.toDp() }
//                val width = with(density) { animatedRect.width.toDp() }
//                val height = with(density) { animatedRect.height.toDp() }


                Box(
                    modifier = Modifier.fillMaxSize(),
//                    modifier = Modifier
//                        .offset(x = offsetX, y = offsetY)
//                        .size(width = width, height = height),
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
                                .size(coil.size.Size.ORIGINAL)


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
//                                .onSizeChanged { if (imageSize == IntSize.Zero) imageSize = it }
                                .graphicsLayer(
                                    scaleX = renderTransform.scale,
                                    scaleY = renderTransform.scale,
                                    translationX = renderTransform.offset.x,
                                    translationY = renderTransform.offset.y,
                                    alpha = 1f - dismissProgress.value  // alpha 从 1 → 0
                                )
                                .drawWithContent {
                                    val imageDisplayRect = calculateImageDisplayRect()

                                    if (isClosing && imageDisplayRect != null) {
                                        val fraction = animateFraction.value

                                        // 起始裁剪区域：图片实际显示区域
                                        val startClip = imageDisplayRect

                                        // 目标裁剪区域：正方形
                                        // 选择正方形边长 = min(图片显示宽度, 图片显示高度)
                                        val squareSize = minOf(
                                            imageDisplayRect.width,
                                            imageDisplayRect.height
                                        )

                                        val endClip = Rect(
                                            left = imageDisplayRect.center.x - squareSize / 2f,
                                            top = imageDisplayRect.center.y - squareSize / 2f,
                                            right = imageDisplayRect.center.x + squareSize / 2f,
                                            bottom = imageDisplayRect.center.y + squareSize / 2f
                                        )

                                        val currentClip = lerpRect(startClip, endClip, fraction)

                                        clipRect(
                                            left = currentClip.left,
                                            top = currentClip.top,
                                            right = currentClip.right,
                                            bottom = currentClip.bottom
                                        ) {
                                            this@drawWithContent.drawContent()
                                        }
                                    } else {
                                        drawContent()
                                    }
                                }
//                                .composed {
//                                    val animate = true
//                                    val fraction = animateFraction.value
//
//                                    Modifier.drawWithContent {
//                                        val contentW = size.width
//                                        val contentH = size.height
//
//                                        // 1️⃣ 起始：完整显示（Fit）
//                                        val startClip = Rect(
//                                            0f,
//                                            0f,
//                                            contentW,
//                                            contentH
//                                        )
//
//                                        // 2️⃣ 目标：正方形裁剪（centerCrop）
//                                        val endClip = centerCropRect(
//                                            src = startClip,
//                                            targetRatio = 1f
//                                        )
//                                        Log.d("animate", startClip.toString())
//                                        Log.d("animate", endClip.toString())
//
//                                        if (animate) {
//                                            val clip = lerpRect(startClip, endClip, fraction)
//
////                                            Log.d("animate", clip.toString())
////                                            Log.d("isAnimateTriggered", isAnimateTriggered.toString())
//                                            clipRect(
//                                                left = clip.left,
//                                                top = clip.top,
//                                                right = clip.right,
//                                                bottom = clip.bottom
//                                            ) {
//                                                this@drawWithContent.drawContent()
//                                            }
//                                        } else {
//                                            drawContent()
//                                        }
//                                    }
//                                }
//                                .drawWithContent {
//
//                                    // ① 计算图片内容在当前 View 里的 Rect（Fit）
//                                    val imageRect = calculateFitRect(
//                                        imageSize = imageSize,
//                                        screenWidth = size.width,
//                                        screenHeight = size.height,
//                                        scale = 1f,
//                                        offset = Offset.Zero
//                                    )
//
//                                    // ② 起始裁剪：图片内容本身
//                                    val startClip = imageRect
//
//                                    // ③ 目标裁剪：正方形 centerCrop（基于图片内容）
//                                    val endClip = centerCropRect(
//                                        src = imageRect,
//                                        targetRatio = 1f
//                                    )
//
//                                    val clip = if (isAnimateTriggered) {
//                                        lerpRect(startClip, endClip, animateFraction.value)
//                                    } else null
//
//                                    Log.d("animate", clip.toString())
//                                    Log.d("isAnimateTriggered", isAnimateTriggered.toString())
//                                    if (clip != null) {
//                                        clipRect(
//                                            left = clip.left,
//                                            top = clip.top,
//                                            right = clip.right,
//                                            bottom = clip.bottom
//                                        ) {
//                                            this@drawWithContent.drawContent()
//                                        }
//                                    } else {
//                                        drawContent()
//                                    }
//                                }

//                                .drawWithContent {
//                                    val contentW = size.width
//                                    val contentH = size.height
//
//                                    // 1️⃣ 起始：完整显示（Fit）
//                                    val startClip = Rect(
//                                        0f,
//                                        0f,
//                                        contentW,
//                                        contentH
//                                    )
//
//                                    // 2️⃣ 目标：正方形裁剪（centerCrop）
//                                    val endClip = centerCropRect(
//                                        src = startClip,
//                                        targetRatio = 1f
//                                    )
//
//                                    val clip = if (isAnimateTriggered) {
//                                        lerpRect(startClip, endClip, animateFraction.value)
//                                    } else null
//
//                                    if (clip != null) {
//                                        clipRect(
//                                            left = clip.left,
//                                            top = clip.top,
//                                            right = clip.right,
//                                            bottom = clip.bottom
//                                        ) {
//                                            this@drawWithContent.drawContent()
//                                        }
//                                    } else {
//                                        drawContent()
//                                    }
//                                }

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
                                            .listener(
                                                onSuccess = { _, result ->
                                                    imageSize = IntSize(
                                                        result.drawable.intrinsicWidth,
                                                        result.drawable.intrinsicHeight
                                                    )
                                                }
                                            )
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
                                SubcomposeAsyncImageContent()

                                // ⭐ 兜底一次 imageSize
                                val painter = painter
                                val drawable = painter.intrinsicSize
                                if (
                                    imageSize == IntSize.Zero && drawable.width > 0f && drawable.height > 0f
                                ) {
                                    imageSize = IntSize(
                                        drawable.width.toInt(),
                                        drawable.height.toInt()
                                    )
                                }
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
