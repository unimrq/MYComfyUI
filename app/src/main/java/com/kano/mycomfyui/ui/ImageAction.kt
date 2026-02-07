package com.kano.mycomfyui.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
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
            val fullUrl = f.file_url.toString()
            if (!f.is_dir && fullUrl.matches(
                    Regex(".*\\.(png|jpg|jpeg|webp)$", RegexOption.IGNORE_CASE)
                )
            ) {
                try {
                    if (creativeMode) {
                        RetrofitClient.getApi().generateImage(
                            type = "脱衣",
                            imageUrl = fullUrl,
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

fun downloadSelectedImages(
    context: Context,
    scope: CoroutineScope,
    selectedImages: List<String>,
    onStart: () -> Unit,
    onProgress: (current: Int, total: Int, filename: String) -> Unit,
    onFinish: () -> Unit,
    onError: (String) -> Unit
) {
    if (selectedImages.isEmpty()) {
        onError("未选中任何图片")
        return
    }

    scope.launch {
        onStart()

        selectedImages.forEachIndexed { index, imagePath ->
            val filename = imagePath.substringAfterLast("/")
            onProgress(index + 1, selectedImages.size, filename)

            try {
                val fullUrl = "${ServerConfig.baseUrl}$imagePath"

                withContext(Dispatchers.IO) {
                    val request = okhttp3.Request.Builder()
                        .url(fullUrl)
                        .build()

                    val response = okhttp3.OkHttpClient()
                        .newCall(request)
                        .execute()

                    if (!response.isSuccessful) {
                        response.close()
                        throw Exception("下载失败")
                    }

                    response.body?.byteStream()?.use { inputStream ->
                        val savedUri = saveFileToGallery(
                            context = context,
                            inputStream = inputStream,
                            filename = filename
                        )

                        if (savedUri == null) {
                            throw Exception("保存失败：$filename")
                        }
                    }

                    response.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("下载出错: ${e.message}")
            }
        }

        onFinish()
    }
}

/**
 * 删除相关
 */

@Composable
fun DeleteConfirmDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("确认删除")
        },
        text = {
            Text("确定要删除选中的 $selectedCount 个文件吗？此操作不可恢复。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun deleteSelectedImages(
    context: Context,
    scope: CoroutineScope,
    multiSelectMode: Boolean,
    selectedImages: MutableList<String>,
    folderContent: FolderContent?,
    imageList: MutableList<String>,
    thumbList: MutableList<String>,
    fileList: SnapshotStateList<String>,
    onSingleDeleteResult: (newIndex: Int, newPath: String?) -> Unit,
    refreshFolder: suspend () -> Unit
) {
    Toast.makeText(context, "正在删除...", Toast.LENGTH_SHORT).show()

    if (multiSelectMode) {
        val filesToDelete = selectedImages.mapNotNull { path ->
            folderContent?.files?.find {
                it.file_url == path || it.path == path
            }
        }

        filesToDelete.forEach { file ->
            val index = imageList.indexOf(file.net_url)
            if (index >= 0) {
                imageList.removeAt(index)
                thumbList.removeAt(index)
                fileList.removeAt(index)
            }
        }

        scope.launch {
            filesToDelete.map { file ->
                async {
                    try {
                        RetrofitClient.getApi().deleteFile(file.path)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.awaitAll()

            selectedImages.clear()
            Toast.makeText(context, "删除完成", Toast.LENGTH_SHORT).show()
            refreshFolder()
        }
    } else {
        val currentPath = selectedImages.firstOrNull() ?: return
        val file = folderContent?.files?.find {
            it.file_url == currentPath || it.path == currentPath
        } ?: return

        val index = imageList.indexOf(file.net_url)
        if (index < 0) return

        imageList.removeAt(index)
        thumbList.removeAt(index)
        fileList.removeAt(index)

        scope.launch {
            try {
                RetrofitClient.getApi().deleteFile(file.path)
                val newIndex = index.coerceAtMost(imageList.lastIndex)
                val newPath = imageList.getOrNull(newIndex)
                onSingleDeleteResult(newIndex, newPath)
                selectedImages.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(context, "删除完成", Toast.LENGTH_SHORT).show()
            refreshFolder()
        }
    }
}
