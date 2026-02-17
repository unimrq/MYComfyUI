package com.kano.mycomfyui.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.kano.mycomfyui.data.FileInfo
import com.kano.mycomfyui.data.FolderContent
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


/**
 * 脱衣动作
 */
suspend fun performNudeGeneration(
    context: Context,
    selectedImages: List<String>,
    folderContent: FolderContent?,
    refreshFolder: () -> Unit,
    clearSelection: () -> Unit,
    creativeMode: Boolean,
    params: Map<String, String>
) {

    var submitted = false

    selectedImages.forEach { path ->
        val file = folderContent?.files?.find {
            it.file_url == path || it.path == path
        }
        file?.let { f ->

            if (!f.is_dir && path.matches(
                    Regex(".*\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE)
                )
            ) {
                try {
                    if (creativeMode) {
                        RetrofitClient.getApi().generateImage(
                            type = "脱衣",
                            imageUrl = path,
                            thumbnailUrl = f.thumbnail_url.toString(),
                            args = params
                        )
                    }
                    submitted = true

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "网络错误: ${f.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🚀 在循环结束后只弹一次
    if (!creativeMode && submitted) {
        Toast.makeText(context, "脱衣任务已提交", Toast.LENGTH_SHORT).show()
    }

    clearSelection()
    refreshFolder()
}


/**
 * 下载相关
 */

fun saveFileToGallery(context: Context, inputStream: InputStream, filename: String): Uri? {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ComfyMobile")
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            inputStream.copyTo(output)
        }
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun resolveDiffFilesWithCheck(
    selectedFiles: List<FileInfo>,
    allFiles: List<FileInfo>
): Pair<FileInfo, FileInfo>? {

    if (selectedFiles.isEmpty()) return null

    val originFile: FileInfo
    val latestNudeFile: FileInfo

    // ==========================================================
    // 1️⃣ 选择 1 张 → 自动匹配
    // ==========================================================
    if (selectedFiles.size == 1) {

        val selectedFile = selectedFiles.first()
        val baseNameWithoutExt = selectedFile.name.substringBeforeLast(".")

        val nudeRegex = Regex("""(.+)-脱衣-(\d+)$""")
        val nudeMatch = nudeRegex.find(baseNameWithoutExt)

        if (nudeMatch != null) {
            // 选中的是脱衣图
            val originalBaseName = nudeMatch.groupValues[1]

            val possibleOrigin = allFiles.find {
                it.name.substringBeforeLast(".") == originalBaseName
            } ?: return null

            originFile = possibleOrigin
            latestNudeFile = selectedFile

        } else {
            // 选中的是原图
            val matchedNudeFiles = allFiles.filter {
                it.name.substringBeforeLast(".")
                    .startsWith("$baseNameWithoutExt-脱衣-")
            }

            if (matchedNudeFiles.isEmpty()) return null

            val latest = matchedNudeFiles.maxByOrNull { file ->
                val timestampPart = file.name
                    .substringBeforeLast(".")
                    .removePrefix("$baseNameWithoutExt-脱衣-")

                timestampPart.toLongOrNull() ?: 0L
            } ?: return null

            originFile = selectedFile
            latestNudeFile = latest
        }
    }

    // ==========================================================
    // 2️⃣ 选择 2 张 → 直接使用
    // ==========================================================
    else if (selectedFiles.size == 2) {
        originFile = selectedFiles[0]
        latestNudeFile = selectedFiles[1]
    }

    else {
        return null
    }

    // ==========================================================
    // 🔍 分辨率校验
    // ==========================================================

    val w1 = originFile.width?.toIntOrNull()
    val h1 = originFile.height?.toIntOrNull()
    val w2 = latestNudeFile.width?.toIntOrNull()
    val h2 = latestNudeFile.height?.toIntOrNull()

    if (w1 == null || h1 == null || w2 == null || h2 == null) {
        return null
    }

    val ratio1 = w1.toFloat() / h1
    val ratio2 = w2.toFloat() / h2
    val ratioDiff = kotlin.math.abs(ratio1 / ratio2 - 1f)

    if (ratioDiff > 0.02f) {
        return null
    }

    return originFile to latestNudeFile
}