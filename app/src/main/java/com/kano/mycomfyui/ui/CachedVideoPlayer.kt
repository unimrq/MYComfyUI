package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.kano.mycomfyui.MyApp
import okhttp3.OkHttpClient

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