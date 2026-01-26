package com.kano.mycomfyui

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

import com.kano.mycomfyui.network.RetrofitClient
import java.io.File

@UnstableApi
class MyApp : Application(), ImageLoaderFactory {


    companion object {
        lateinit var simpleCache: SimpleCache
        lateinit var instance: MyApp
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        RetrofitClient.init(this)
        val videoCacheDir = File(filesDir, "video_cache")
        if (!videoCacheDir.exists()) videoCacheDir.mkdirs()

        // Media3 的 LRU Evictor
        val evictor = LeastRecentlyUsedCacheEvictor(1024L * 1024 * 1024) // 1GB
        simpleCache = SimpleCache(videoCacheDir, evictor)
    }

    override fun newImageLoader(): ImageLoader {
        val imageCacheDir = File(filesDir, "image_cache")
        if (!imageCacheDir.exists()) imageCacheDir.mkdirs()

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 使用 25% 内存缓存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDir)
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // 忽略HTTP缓存头
            .build()
    }
}
