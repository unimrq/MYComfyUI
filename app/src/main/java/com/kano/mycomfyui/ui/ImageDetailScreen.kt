package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import androidx.annotation.OptIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.kano.mycomfyui.MyApp
import com.kano.mycomfyui.network.OkHttpProvider
import kotlin.math.abs
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kano.mycomfyui.network.ServerConfig


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


@SuppressLint("UnrememberedMutableState")
@Composable
fun ImageDetailScreen(
    imagePaths: MutableList<String>,
    filePaths: MutableList<String>,
    thumbPaths: MutableList<String>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onGenerateClick: ((String) -> Unit)? = null,
    onSelectedFileChange: ((filePath: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(initialIndex) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(currentIndex) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)

                    // 更新缩放
                    scale = newScale

                    if (scale > 1f) {
                        // ✅ 放大状态 → 拖动平移图片
                        val newOffset = offset + pan
                        val scaledWidth = imageSize.width * scale
                        val scaledHeight = imageSize.height * scale
                        val maxOffsetX = maxOf((scaledWidth - containerSize.width) / 2f, 0f)
                        val maxOffsetY = maxOf((scaledHeight - containerSize.height) / 2f, 0f)
                        offset = Offset(
                            newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else if (zoom == 1f) {
                        // ✅ 原始比例下记录滑动偏移
                        dragAccumulation += pan

                        // 左右滑切图
                        if (abs(dragAccumulation.x) > 120f) {
                            if (dragAccumulation.x > 0 && currentIndex > 0) {
                                currentIndex--
                            } else if (dragAccumulation.x < 0 && currentIndex < imagePaths.lastIndex) {
                                currentIndex++
                            }
                            dragAccumulation = Offset.Zero
                            onSelectedFileChange?.invoke(imagePaths[currentIndex])
                        }

                        // 下滑关闭
                        if (dragAccumulation.y > 100f) {
                            onClose()
                            dragAccumulation = Offset.Zero
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // ✅ 双击放大/还原
                detectTapGestures(
                    onTap = {
                        if(!isVideo){
                            onGenerateClick?.invoke(imagePaths[currentIndex])
                        }
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
    ) {
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val isGif = imagePath.lowercase().endsWith(".gif")
                ImageRequest.Builder(context)
                    .data(imagePath)
                    .size(Size.ORIGINAL)
                    .apply {
                        if (isGif) {
                            decoderFactory(ImageDecoderDecoder.Factory())
                        }
                    }
                    .build()

                val thumbPath = thumbPaths.getOrNull(currentIndex)

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imagePath)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .size(Size.ORIGINAL)
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
                        .onSizeChanged { imageSize = it }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .fillMaxSize(),
                    loading = {
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

                Text(
                    text = "${currentIndex + 1} / ${imagePaths.size}",
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
