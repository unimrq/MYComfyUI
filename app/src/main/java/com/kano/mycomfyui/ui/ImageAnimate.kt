package com.kano.mycomfyui.ui

import android.util.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

class ImageAnimate {
}

data class ImageBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun toRect(): Rect {
        return Rect(
            left,
            top,
            left + width,
            top + height
        )
    }
}


//fun lerp(start: Float, stop: Float, fraction: Float): Float {
//    return start + (stop - start) * fraction
//}

fun calculateFitRect(
    imageSize: IntSize,
    screenWidth: Float,
    screenHeight: Float
): Rect {
    val imageWidth = imageSize.width.toFloat()
    val imageHeight = imageSize.height.toFloat()

    val imageRatio = imageWidth / imageHeight
    val screenRatio = screenWidth / screenHeight

    return if (imageRatio > screenRatio) {
        val width = screenWidth
        val height = width / imageRatio
        val top = (screenHeight - height) / 2f

        Rect(
            left = 0f,
            top = top,
            right = width,
            bottom = top + height
        )
    } else {
        val height = screenHeight
        val width = height * imageRatio
        val left = (screenWidth - width) / 2f

        Rect(
            left = left,
            top = 0f,
            right = left + width,
            bottom = height
        )
    }
}

