package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.kano.mycomfyui.R
import com.kano.mycomfyui.data.FileInfo
import com.kano.mycomfyui.data.FolderContent
import com.kano.mycomfyui.network.OkHttpProvider
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt


/**
 * 脱衣动作
 */
suspend fun performNudeGeneration(
    context: Context,
    selectedImages: List<String>,
    folderContent: FolderContent?,
    refreshFolder: () -> Unit,
    clearSelection: () -> Unit,
    creativeMode: Boolean
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
                            type = "换衣_创意",
                            imageUrl = fullUrl,
                            thumbnailUrl = f.thumbnail_url.toString(),
                            args = emptyMap()
                        )
                    } else {
                        RetrofitClient.getApi().generateImage(
                            type = "换衣_蒙版",
                            imageUrl = fullUrl,
                            thumbnailUrl = f.thumbnail_url.toString(),
                            args = emptyMap()
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
        Toast.makeText(context, "换衣任务已提交", Toast.LENGTH_SHORT).show()
    }

    clearSelection()
    refreshFolder()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudeModeBottomSheet(
    maskEnabled: Boolean,
    onDismiss: () -> Unit,
    onMaskModeClick: () -> Unit,
    onCreativeModeClick: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (maskEnabled) {
                    Button(
                        onClick = onMaskModeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("蒙版模式")
                    }
                }

                Button(
                    onClick = onCreativeModeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffb3424a)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("创意模式")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
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
