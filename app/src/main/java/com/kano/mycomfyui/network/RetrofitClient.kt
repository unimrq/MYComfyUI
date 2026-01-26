package com.kano.mycomfyui.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object OkHttpProvider {

    fun create(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
    }
}


object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null
    private lateinit var appContext: Context

    // 每次获取 ApiService 时都使用最新 Retrofit
    fun getApi(): ApiService {
        return getRetrofit().create(ApiService::class.java)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
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
            .baseUrl(ServerConfig.baseUrl)
            .client(OkHttpProvider.create(appContext))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}



