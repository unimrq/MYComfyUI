package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.kano.mycomfyui.R
import com.kano.mycomfyui.data.FileInfo
import kotlinx.coroutines.launch
import kotlin.math.pow


@SuppressLint("UnrememberedMutableState", "ConfigurationScreenWidthHeight",
    "UnnecessaryComposedModifier"
)
@Composable
fun ImageDetailScreen(
    sortedFiles: List<FileInfo>,
    initialIndex: Int = 0,
    onImageClick: () -> Unit,
    onSelectedFileChange: ((String) -> Unit)? = null,
    visibleCoordsMap: SnapshotStateMap<String, LayoutCoordinates>,  // ✅ 新增
    onRequestClose: () -> Unit,
    onCloseAnimationEnd: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var isTopBarVisible by remember { mutableStateOf(true) }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    if (sortedFiles.isEmpty()) return

    fun getSafeFile(
        files: List<FileInfo>,
        index: Int
    ): FileInfo? {
        return when {
            index in files.indices -> files[index]
            files.isNotEmpty() -> files.last() // 或 first，看你 UX
            else -> null
        }
    }

    val currentFile = getSafeFile(sortedFiles, currentIndex) ?: return


//    Log.d("imagepath", currentFile.net_url.toString())

    // 缩放和平移
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f
    val doubleTapScale = 3f

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val widthInt = currentFile.width?.toIntOrNull() ?: 0
    val heightInt = currentFile.height?.toIntOrNull() ?: 0
    val size = IntSize(widthInt, heightInt)
    var imageSize by remember { mutableStateOf(size) }

    // 动画
    // 下拽相关
    var dragY by remember { mutableStateOf(0f) }
    var isDraggingDown by remember { mutableStateOf(false) }
    var dragYAddAble by remember { mutableStateOf(true) }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // 阈值
    val animateThreshold = screenHeightPx * 0.5f
    val closeThreshold = screenHeightPx * 0.05f
    var isClosing by remember { mutableStateOf(false) }

    var isAnimateTriggered by remember { mutableStateOf(false) }
    var isCloseTriggered by remember { mutableStateOf(false) }
    val dragScale by derivedStateOf {
        if (!isAnimateTriggered) {
            val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
            androidx.compose.ui.util.lerp(start = 1f, stop = 0.3f, fraction = dragProgress)
        } else {
            0.5f
        }
    }

    val backgroundAlpha  by derivedStateOf {
        if (!isAnimateTriggered) {
            val dragProgress = (dragY / animateThreshold).coerceIn(0f, 1f)
            // 非线性快速衰减
            1f - dragProgress.pow(1f)
        } else {
            0f
        }
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
    val coords = currentFile
        .path
        .let { key ->
            visibleCoordsMap[key]
        }

//    Log.d("ImageDetailCoords", visibleCoordsMap.toString())


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

    data class Transform(
        val scale: Float = 1f,
        val offset: Offset = Offset.Zero
    )

    data class CloseAnimSnapshot(
        val startRect: Rect = Rect.Zero,
        val endRect: Rect = Rect.Zero
    )


    var closeSnapshot by remember { mutableStateOf<CloseAnimSnapshot?>(null) }

    fun rectToRectTransform(
        start: Rect,
        end: Rect,
    ): Transform {

        val scaleX = end.width / start.width
        val scaleY = end.height / start.height

        val scale = maxOf(scaleX, scaleY)
        val startCenter = start.center
        val endCenter = end.center

        val targetOffset = endCenter - startCenter

        return Transform(
            scale = scale,
            offset = targetOffset
        )
    }

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

    val maskColor = if (isTopBarVisible) Color.White else Color.Black
    val alpha1 = backgroundAlpha * (1f - animateFraction.value)

    Box(
        Modifier.fillMaxSize()
    ) {
        // 背景遮罩
        Box(
            Modifier
                .matchParentSize()
                .zIndex(if (isTopBarVisible) 0f else 2f)
                .graphicsLayer { alpha = alpha1 }
                .background(maskColor)
        )

        // 原有内容（不受影响）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .zIndex(if(isTopBarVisible) 0f else 2f)
                .pointerInput(currentIndex) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                        if (!isAnimateTriggered) {
                            scale = newScale
                            if (scale > 1f) {
                                // 放大状态 → 平移图片
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

                                } else if (pan.y < 0){
                                    if(isCloseTriggered){
                                        isAnimateTriggered = false
                                        offset += pan / newScale
                                        dragYAddAble = false
                                    } else {
                                        offset += pan / newScale
//                                        scope.launch {
//                                            offsetAnim.snapTo(offset)
//                                            offsetAnim.animateTo(Offset.Zero, animationSpec = tween(200, easing = FastOutSlowInEasing)) {
//                                                offset = value
//                                            }
//                                        }
                                    }
                                }
                            }
                        } else {
                            offset += pan / newScale
                        }
                    }
                }
                .pointerInput(Unit) {
                    // ✅ 双击放大/还原
                    detectTapGestures(
                        onTap = {
                            onImageClick()
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
                                if (!isCloseTriggered && scale == 1f) {
                                    scope.launch {
                                        offsetAnim.snapTo(offset)
                                        offsetAnim.animateTo(
                                            Offset.Zero,
                                            animationSpec = tween(
                                                durationMillis = 200,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) {
                                            offset = value
                                        }
                                    }
                                }
                                if (isCloseTriggered && !isClosing) {
                                    isClosing = true
                                    onRequestClose()

                                    scope.launch {
                                        animateFraction.snapTo(0f)

                                        animateFraction.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(
                                                durationMillis = 360,
                                                easing = FastOutSlowInEasing
                                            )
                                        )

                                        onCloseAnimationEnd()
                                    }
                                }
                                else {
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
                    lerpTransform(currentTransform, targetTransform, animateFraction.value)
                } else {
                    currentTransform
                }


            val pagerState = rememberPagerState(initialPage = currentIndex, pageCount = { sortedFiles.size })
            val userScrollEnabled = scale == 1f

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp,
                userScrollEnabled = userScrollEnabled
            ) { page ->
                var showRawImage by remember(page) { mutableStateOf(false) }
                val currentFile = sortedFiles.getOrNull(page)

                val thumbPath = currentFile?.thumb_url
                val imagePath = if (showRawImage) {
                    currentFile?.net_url?.replace("/photos/", "/photos-raw/")
                } else {
                    currentFile?.net_url
                }


                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .collect { page ->
                            if (page != currentIndex) {
                                currentIndex = page
                                val file = sortedFiles.getOrNull(currentIndex)
                                file?.net_url?.let { onSelectedFileChange?.invoke(it) }
                            }
                        }

                }

                LaunchedEffect(imagePath) {
                    scale = 1f
                    offset = Offset.Zero
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imagePath)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .size(Size.ORIGINAL)
                            .build(),
                        contentDescription = "大图",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = renderTransform.scale,
                                scaleY = renderTransform.scale,
                                translationX = renderTransform.offset.x,
                                translationY = renderTransform.offset.y,
                            )
                            .drawWithContent {
                                val imageDisplayRect = calculateImageDisplayRect()

                                if (isClosing && imageDisplayRect != null) {
                                    val fraction = animateFraction.value

                                    // 起始裁剪区域：图片实际显示区域
                                    val startClip = imageDisplayRect

                                    // 目标裁剪区域：正方形
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
                            .fillMaxSize(),
                        loading = {
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
                                    contentDescription = "缩略图",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                // 缩略图不存在时显示加载指示器
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.Black)
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

                    IconButton(
                        onClick = { showRawImage = !showRawImage },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 96.dp)
                            .size(32.dp).graphicsLayer { alpha = 0f }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0f),
                                    shape = CircleShape
                                )
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            if (showRawImage) {
                                Image(
                                    painter = painterResource(id = R.drawable.visibility),
                                    contentDescription = "查看原图",
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = if (isTopBarVisible) ColorFilter.tint(Color.Black) else ColorFilter.tint(Color.White)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.visibility_off),
                                    contentDescription = "查看原图",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = if (isTopBarVisible) ColorFilter.tint(Color.Black) else ColorFilter.tint(Color.White)
                                )
                            }

                        }
                    }
                }
            }

            // ✅ 页码固定在屏幕底部中央
            if(!isTopBarVisible){
                Text(
                    text = "${pagerState.currentPage + 1} / ${sortedFiles.size}",
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
