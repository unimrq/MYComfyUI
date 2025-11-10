package com.kano.mycomfyui.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    // 每次获取 ApiService 时都使用最新 Retrofit
    fun getApi(): ApiService {
        return getRetrofit().create(ApiService::class.java)
    }

    private fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit().also { retrofit = it }
        }
    }

    // 重建 Retrofit
    fun rebuildRetrofit() {
        synchronized(this) {
            retrofit = buildRetrofit()
        }
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ServerConfig.baseUrl) // 一定要保证末尾 "/"
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

