package com.kano.mycomfyui.ui

import android.content.Context

private const val PREFS_NAME = "function_settings"
private const val KEY_VIDEO_GEN = "enable_video_gen"
private const val KEY_MASK_CLOTHES = "enable_mask_clothes"

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

// 保存 蒙版换衣 开关
fun saveMaskClothesEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_MASK_CLOTHES, enabled).apply()
}

// 读取 蒙版换衣 开关
fun loadMaskClothesEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_MASK_CLOTHES, false)
}
