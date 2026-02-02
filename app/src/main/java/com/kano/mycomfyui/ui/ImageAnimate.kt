package com.kano.mycomfyui.ui

import androidx.compose.ui.geometry.Offset
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


data class ScaleOffset(val scaleX: Float, val scaleY: Float, val offsetX: Float, val offsetY: Float)


fun lerpScaleOffset(startRect: Rect, endRect: Rect, fraction: Float): ScaleOffset {
    // scale = 目标宽高 / 起始宽高
    val scaleX = androidx.compose.ui.util.lerp(1f, endRect.width / startRect.width, fraction)
    val scaleY = androidx.compose.ui.util.lerp(1f, endRect.height / startRect.height, fraction)

    // offset = 中心点的偏移
    val startCenter = startRect.center
    val endCenter = endRect.center
    val offsetX = androidx.compose.ui.util.lerp(0f, endCenter.x - startCenter.x, fraction)
    val offsetY = androidx.compose.ui.util.lerp(0f, endCenter.y - startCenter.y, fraction)

    return ScaleOffset(scaleX, scaleY, offsetX, offsetY)
}



//fun lerp(start: Float, stop: Float, fraction: Float): Float {
//    return start + (stop - start) * fraction
//}

fun calculateFitRect(
    imageSize: IntSize,
    screenWidth: Float,
    screenHeight: Float,
    scale: Float = 1f,
    offset: Offset = Offset.Zero
): Rect {
    val imageWidth = imageSize.width.toFloat()
    val imageHeight = imageSize.height.toFloat()

    val imageRatio = imageWidth / imageHeight
    val screenRatio = screenWidth / screenHeight

    val baseRect = if (imageRatio > screenRatio) {
        val width = screenWidth
        val height = width / imageRatio
        val top = (screenHeight - height) / 2f
        Rect(0f, top, width, top + height)
    } else {
        val height = screenHeight
        val width = height * imageRatio
        val left = (screenWidth - width) / 2f
        Rect(left, 0f, left + width, height)
    }

    // 缩放
    val centerX = baseRect.center.x
    val centerY = baseRect.center.y
    val halfWidth = baseRect.width / 2f * scale
    val halfHeight = baseRect.height / 2f * scale

    return Rect(
        left = centerX - halfWidth + offset.x,
        top = centerY - halfHeight + offset.y,
        right = centerX + halfWidth + offset.x,
        bottom = centerY + halfHeight + offset.y
    )

}

