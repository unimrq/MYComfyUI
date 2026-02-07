package com.kano.mycomfyui.ui

import android.content.Context

private const val PREFS_NAME = "function_settings"
private const val KEY_VIDEO_GEN = "enable_video_gen"
private const val TEXT_TO_IMAGE = "enable_text2img"

// 保存 图生视频 开关
fun saveVideoGenEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_VIDEO_GEN, enabled).apply()
}

// 读取 图生视频 开关
fun loadVideoGenEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_VIDEO_GEN, false)
}


fun saveText2ImgEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(TEXT_TO_IMAGE, enabled).apply()
}

// 读取 蒙版换衣 开关
fun loadText2ImgEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(TEXT_TO_IMAGE, false)
}


