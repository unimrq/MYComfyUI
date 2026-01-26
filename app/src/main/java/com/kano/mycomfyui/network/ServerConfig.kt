package com.kano.mycomfyui.network

import android.content.Context
import com.google.gson.Gson
import com.kano.mycomfyui.ui.ServerAddress

object ServerConfig {
    // 默认外网地址（先用这个）
    var baseUrl: String = "http://172.93.187.227:12345/"
    var secret: String = ""
}