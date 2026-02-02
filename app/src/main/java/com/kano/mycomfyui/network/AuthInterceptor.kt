package com.kano.mycomfyui.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val secret = ServerConfig.secret

        val request = if (secret.isNotBlank()) {
            chain.request()
                .newBuilder()
                .addHeader("X-Secret", secret)
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
