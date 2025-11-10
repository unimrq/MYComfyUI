package com.kano.mycomfyui

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import java.io.File

class MyApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var simpleCache: SimpleCache
        lateinit var instance: MyApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val videoCacheDir = File(filesDir, "video_cache")
        if (!videoCacheDir.exists()) videoCacheDir.mkdirs()

        val evictor = LeastRecentlyUsedCacheEvictor(1024L * 1024 * 1024)
        val dbProvider = ExoDatabaseProvider(this)
        simpleCache = SimpleCache(videoCacheDir, evictor, dbProvider)
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
