package com.kano.mycomfyui.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.kano.mycomfyui.R
import com.kano.mycomfyui.data.FileInfo
import com.kano.mycomfyui.data.FolderContent
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumScreen(
    paddingValues: PaddingValues,
    onExitApp: () -> Unit,
    navController: NavHostController,
    onLockClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var folderContent by remember { mutableStateOf<FolderContent?>(null) }
    val context = LocalContext.current
    var selectedFileForMenu by remember { mutableStateOf<FileInfo?>(null) }
    var confirmDeleteDialogVisible by remember { mutableStateOf(false) }
    var pendingDeleteFile by remember { mutableStateOf<FileInfo?>(null) }
    var showGenerateSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    val gson = Gson()
    val prefs: SharedPreferences = context.getSharedPreferences("album_cache", Context.MODE_PRIVATE)
    val prefs1: SharedPreferences = context.getSharedPreferences("path_cache", Context.MODE_PRIVATE)
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedImages = remember { mutableStateListOf<String>() }
    val pathOptions = listOf(
        "最新" to "动图/最新",
        "修图" to "修图",
        "素材" to "素材",
        "动图" to "动图",
        )
    var currentTab by rememberSaveable { mutableStateOf("素材") }
    var generateImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var generateThumbnailUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNudeSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    var readyToDisplay by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val topBarColor = remember { mutableStateOf(Color.Black) }
    val maskVisible = remember { mutableStateOf(0f) }

    val maskHeight = 100.dp
    val maskTriggerPx = with(LocalDensity.current) { 140.dp.toPx() }
    val maskHeightPx = with(LocalDensity.current) { maskHeight.toPx() }
    val spacerHeightPx = with(LocalDensity.current) { 120.dp.toPx() } // Grid 上方 Spacer
    val thresholdPx = with(LocalDensity.current) { 170.dp.toPx() }
    val scrollPositions = remember {
        mutableStateMapOf<String, Pair<Int, Int>>()
    }
    val view = LocalView.current
    val window = (view.context as Activity).window
    val currentImageIndex = remember { mutableStateOf(0) }
    var progressVisible by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    val imageList = remember { mutableStateListOf<String>() }
    val thumbList = remember { mutableStateListOf<String>() }
    val fileList = remember { mutableStateListOf<String>() }
    // 剪切板：存放待移动的文件
    var cutList by remember { mutableStateOf<List<String>>(emptyList()) }
    var cutSourceDir by remember { mutableStateOf("") }
    val refreshState = rememberPullToRefreshState()
    fun saveFolderCache(path: String, content: FolderContent) {
        val json = gson.toJson(content)
        prefs.edit { putString(path, json) }
    }

    fun getFolderCache(path: String): FolderContent? {
        return prefs.getString(path, null)?.let { gson.fromJson(it, FolderContent::class.java) }
    }

    fun savePath(key: String, path: String) {
        prefs1.edit { putString(key, path) }
    }

    fun getSavedPath(key: String, defaultPath: String): String {
        return prefs1.getString(key, defaultPath) ?: defaultPath
    }

    var currentPath by rememberSaveable {
        mutableStateOf(getSavedPath(currentTab, pathOptions.find { it.first == currentTab }!!.second))
    }


    suspend fun uploadImageFromUri(uri: Uri) {
        try {
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            // 构造 Multipart
            val requestFile = file.asRequestBody("image/*".toMediaType())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val pathBody = RequestBody.create("text/plain".toMediaType(), currentPath)

            // 发起上传请求并解析响应
            val response = RetrofitClient.getApi().uploadImage(pathBody, body)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                when {
                    response.code() == 400 && errorBody.contains("500 张图片上限") -> {
                        Toast.makeText(context, "该文件夹已达 500 张图片上限", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "上传失败：${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


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

    fun rememberDirectory(nowPath: String, newPath: String) {
        // 保存当前目录滚动位置
        val pos = gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        scrollPositions[nowPath] = pos
        // 初始化新路径滚动位置
        if (!scrollPositions.containsKey(newPath)) {
            scrollPositions[newPath] = 0 to 0
        }
    }

    // 拉取 API
    suspend fun refreshFolder() {
        val requestedPath = currentPath

        // 1️⃣ 尝试读取本地缓存
        getFolderCache(requestedPath)?.let { cached ->
            folderContent = cached
        }
        savePath(currentTab, requestedPath)

        try {
            // 2️⃣ 异步刷新服务器
//            RetrofitClient.apiService.refresh(requestedPath)
            val serverContent = RetrofitClient.getApi().browse(requestedPath)

            // 3️⃣ 路径一致时才更新
            if (requestedPath == currentPath) {
                val oldJson = gson.toJson(folderContent)
                val newJson = gson.toJson(serverContent)
                if (oldJson != newJson) {
                    folderContent = serverContent
                    saveFolderCache(requestedPath, serverContent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (requestedPath == currentPath) {
                Toast.makeText(context, "刷新失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                // 倒序上传
                totalCount = uris.size
                progressVisible = true
                uris.asReversed().forEachIndexed { index, uri ->
                    currentIndex = index + 1
                    currentFileName = uri.lastPathSegment ?: "image_${index + 1}"

                    try {
                        uploadImageFromUri(uri)
                    } catch (e: Exception) {
                        Toast.makeText(context, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                progressVisible = false
                refreshFolder()
            }
        }
    }

    LaunchedEffect(multiSelectMode) {
        if (!multiSelectMode) {
            selectedImages.clear()
        }
    }


    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (firstIndex, offset) ->
                val itemHeightPx = 200f // 每个 Grid item 高度

                // totalScroll = 实际滚动 + Spacer 高度（只加一次）+ 状态栏高度
                val totalScroll = firstIndex * itemHeightPx + offset + spacerHeightPx

                maskVisible.value = ((totalScroll - maskTriggerPx) / maskHeightPx).coerceIn(0f, 1f)

                topBarColor.value = if (totalScroll > thresholdPx) Color.White else Color.Black
            }
    }

    LaunchedEffect(maskVisible.value) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        // 当 maskVisible 超过一定阈值时切换为浅色图标
        if (maskVisible.value >= 0.8f) {
            insetsController.isAppearanceLightStatusBars = false // 白色图标
        } else {
            insetsController.isAppearanceLightStatusBars = true  // 黑色图标
        }
    }

    LaunchedEffect(Unit) {
        refreshFolder()
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                rememberDirectory(currentPath, currentPath)
            }
        })
    }

    LaunchedEffect(folderContent?.files, folderContent?.folders) {
        val pos = scrollPositions[currentPath]
        if (pos != null) {
            gridState.scrollToItem(pos.first, pos.second)
        }
    }

    LaunchedEffect(currentPath) {
        folderContent?.let { content ->
            // 取文件夹名并限制长度
            val folderName = run {
                // 如果是当前目录
                if (currentPath == "." || currentPath.isBlank()) {
                    "根目录"
                } else {
                    // 用反斜杠分割路径
                    val parts = currentPath.split('/').filter { it.isNotBlank() }
                    val name = when {
                        parts.isEmpty() -> "根目录"
                        parts.size == 1 -> parts.last()
                        else -> parts.takeLast(2).joinToString("/")  // 取最后两级
                    }
                    if (name.length > 12) name.take(12) + "…" else name
                }
            }
        }
    }


    BackHandler(enabled = true) {
        when {
            multiSelectMode -> {
                multiSelectMode = false
                selectedImages.clear()
            }

            previewImagePath != null -> {
                // 如果正在预览大图，则先关闭预览
                previewImagePath = null
            }

            currentTab != "最新" && currentPath != "素材" && currentPath != "动图" && currentPath != "修图" && folderContent?.parent != null -> {
                // 非最新Tab，且有父目录时返回上一级
                rememberDirectory(currentPath, folderContent!!.parent.path)
                currentPath = folderContent!!.parent.path
                scope.launch {
                    refreshFolder()
                }
            }

            else -> {
                // 否则退出应用
                onExitApp()
            }
        }
    }

    Scaffold(
        modifier = Modifier.background(Color.White),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (previewImagePath == null){
                val displayPath = currentPath.split("/")
                    .takeLast(2)
                    .joinToString("·")

                // 简单根据字符数控制字体大小
                val fontSize = when {
                    displayPath.length <= 10 -> 20.sp
                    displayPath.length <= 20 -> 18.sp
                    displayPath.length <= 30 -> 16.sp
                    else -> 14.sp
                }

                TopAppBar(
                    title = {
                        Text(
                            text = displayPath,
                            color = topBarColor.value,
                            fontSize = fontSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (multiSelectMode) {
                                IconButton(onClick = {
                                    if (selectedImages.size == folderContent?.files?.count { !it.is_dir }) {
                                        // 如果已经全选，则清空
                                        selectedImages.clear()
                                    } else {
                                        // 否则选中所有图片
                                        selectedImages.clear()
                                        selectedImages.addAll(
                                            folderContent?.files
                                                ?.filter { !it.is_dir }
                                                ?.map { it.file_url ?: it.path }
                                                ?: emptyList()
                                        )
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "全选",
                                        tint = topBarColor.value
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    showAddSheet = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新增",
                                        tint = topBarColor.value
                                    )
                                }
                            }


                            var expanded by remember { mutableStateOf(false) }

                            IconButton(onClick = {
                                expanded = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = topBarColor.value
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .width(90.dp)
                                    .background(Color.White),
                            ) {

                                DropdownMenuItem(
                                    text = { Text("任务管理") },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("taskManage")
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("刷新页面") },
                                    onClick = {
                                        expanded = false
                                        scope.launch {
                                            RetrofitClient.getApi().refresh(currentPath)
                                            refreshFolder()
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("地址设置") },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("settings")
                                    }
                                )
                            }

                        }
                    },
                    colors = topAppBarColors(
                        containerColor = Color.Transparent,   // 背景透明
                        titleContentColor = Color.White,      // 标题白色
                        actionIconContentColor = Color.White  // 图标白色
                    ),
                    modifier = Modifier
                        .shadow(0.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    onLockClick()
                                }
                            )
                        }
                )
            }
        },
    ) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val swipeThreshold = screenWidthPx / 4f

        Box (

            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(currentTab, multiSelectMode) {

                    if (!multiSelectMode) { // 多选模式下不响应滑动
                        detectHorizontalDragGestures { change, dragAmount ->
                            val currentIndex = pathOptions.indexOfFirst { it.first == currentTab }
                            if (dragAmount > 30 && currentIndex > 0) { // 向右
                                val newTab = pathOptions[currentIndex - 1]
                                currentTab = newTab.first
                                rememberDirectory(
                                    currentPath,
                                    getSavedPath(newTab.first, newTab.second)
                                )
                                currentPath = getSavedPath(newTab.first, newTab.second)
                                scope.launch {
                                    refreshFolder()
                                }
                            } else if (dragAmount < -30 && currentIndex < pathOptions.size - 1) { // 向左
                                val newTab = pathOptions[currentIndex + 1]
                                currentTab = newTab.first
                                rememberDirectory(
                                    currentPath,
                                    getSavedPath(newTab.first, newTab.second)
                                )
                                currentPath = getSavedPath(newTab.first, newTab.second)
                                scope.launch {
                                    refreshFolder()
                                }
                            }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    // 图片/文件夹网格
                    folderContent?.let { content ->

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                        fun getCoreName(name: String): String {
                            var base = name.substringBeforeLast(".")      // 去掉扩展名
                            base = base.substringBefore("-换衣")           // 截断 -换衣 及之后
                            base = base.replace(Regex("[ab]$"), "")       // 去掉末尾 a 或 b
                            return base
                        }

                        // 统一排序一次
                        val sortedFiles = content.files
                            .filter { !it.is_dir }
                            .let { files ->
                                // 如果全部或部分是 mp4，则按时间排序
                                val isMp4Only = files.all { it.file_url?.lowercase()?.endsWith(".mp4") == true }
                                if (isMp4Only || currentPath == "修图") {
                                    files.sortedByDescending { file ->
                                        runCatching {
                                            dateFormat.parse(file.date)?.time ?: Long.MAX_VALUE
                                        }.getOrDefault(Long.MAX_VALUE)
                                    }
                                } else {
                                    // 之前的分组排序逻辑（非 mp4 文件）
                                    files
                                        .groupBy { getCoreName(it.name) }
                                        .toList()
                                        .sortedByDescending { (_, groupFiles) ->
                                            groupFiles.minOfOrNull { file ->
                                                runCatching { dateFormat.parse(file.date)?.time ?: Long.MAX_VALUE }.getOrDefault(Long.MAX_VALUE)
                                            } ?: Long.MAX_VALUE
                                        }
                                        .flatMap { (_, groupFiles) ->
                                            groupFiles.sortedWith(compareBy<FileInfo> {
                                                runCatching { dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE }
                                                    .getOrDefault(Long.MAX_VALUE)
                                            }.thenBy { it.name.lowercase() })
                                        }
                                }
                            }


                        val sortedFolders = content.folders
                            .map { FileInfo(name = it.name, is_dir = true, path = it.path) }
                            .sortedBy { it.name.lowercase() } // 按名称排序

                        val allItems = sortedFolders + sortedFiles
                        val fileCoordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }

                        if (readyToDisplay) {
                            if (previewImagePath == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(maskHeight)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Black, Color.Transparent)
                                            ),
                                            alpha = maskVisible.value * 0.8f // 最大 0.8
                                        )
                                        .align(Alignment.TopStart) // BoxScope 内
                                        .zIndex(1f)
                                )
                            }


                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        refreshFolder()
                                        isRefreshing = false
                                    }
                                },
                                state = refreshState,
                                indicator = {
                                    Indicator(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 120.dp),
                                        isRefreshing = isRefreshing,
                                        containerColor = Color.White,
                                        color = Color(0xFF0066FF),
                                        state = refreshState,
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                GridWithVerticalScrollHandleOverlay(allItems = allItems, columns = 3, handleHeight = 40.dp, gridState = gridState) {


                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentPadding = PaddingValues(top = 120.dp, bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {

                                        items(allItems, key = { it.path }) { file ->
                                            val fullUrl = file.file_url?.let { "${ServerConfig.baseUrl}$it" }
                                            Column(
                                                modifier = Modifier
                                                    .onGloballyPositioned { coords ->
                                                        fileCoordsMap[file.path] = coords
                                                    }
                                                    .aspectRatio(1f)
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (multiSelectMode && !file.is_dir) {
                                                                // 多选模式：点击切换选中状态
                                                                val path =
                                                                    file.file_url ?: file.path
                                                                if (selectedImages.contains(path)) {
                                                                    selectedImages.remove(path)
                                                                } else {
                                                                    selectedImages.add(path)
                                                                }
                                                            } else {
                                                                // 普通模式
                                                                if (file.is_dir) {
                                                                    rememberDirectory(
                                                                        currentPath,
                                                                        file.path
                                                                    )
                                                                    currentPath = file.path
                                                                    scope.launch {
                                                                        refreshFolder()
                                                                    }
                                                                } else fullUrl?.let {
                                                                    previewImagePath = it
                                                                }

                                                                if (!file.is_dir) {
                                                                    selectedFileForMenu = file
                                                                }
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!file.is_dir) {
                                                                multiSelectMode = true
                                                                selectedImages.add(
                                                                    file.file_url ?: file.path
                                                                )
                                                            }
                                                        }
                                                    ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                if (file.is_dir) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentHeight(),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Surface(
                                                            tonalElevation = 2.dp,
                                                            shadowElevation = 6.dp,
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFEFEFEF), // ✅ 指定背景色，避免默认白底透出
                                                            modifier = Modifier.size(48.dp)
                                                        ) {
                                                            Image(
                                                                painter = painterResource(id = R.drawable.folder),
                                                                contentDescription = "Folder",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop // ✅ 填充整个圆角区域
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text(
                                                            text = file.name,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(36.dp)
                                                                .padding(horizontal = 8.dp)
                                                        )
                                                    }
                                                } else {
                                                    val isVideo =
                                                        file.file_url?.lowercase()?.endsWith(".mp4") == true
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(file.thumbnail_url?.let { "${ServerConfig.baseUrl}$it" }
                                                                    ?: file.file_url)
                                                                .diskCacheKey(file.file_url ?: file.path)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .networkCachePolicy(CachePolicy.ENABLED)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = file.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(0.dp))
                                                                .background(Color.LightGray)
                                                        )

                                                        val path = file.file_url ?: file.path

                                                        if (selectedImages.contains(path)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(
                                                                        Color.White.copy(
                                                                            alpha = 0.3f
                                                                        )
                                                                    ) // 半透明白色遮罩
                                                            )
                                                        }

                                                        if (multiSelectMode) {

                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp)
                                                                    .size(20.dp)
                                                                    .border(
                                                                        width = 2.dp,
                                                                        color = Color.White, // 蓝色边框
                                                                        shape = RoundedCornerShape(2.dp)
                                                                    )
                                                                    .background(
                                                                        color = if (selectedImages.contains(
                                                                                path
                                                                            )
                                                                        ) Color(
                                                                            0xFFEE8E00
                                                                        ) else Color.Transparent, // 蓝色背景
                                                                        shape = RoundedCornerShape(2.dp)
                                                                    ),
                                                                contentAlignment = Alignment.Center

                                                            ) {
                                                                if (selectedImages.contains(path)) {
                                                                    Icon(
                                                                        Icons.Default.Check,
                                                                        contentDescription = "Selected",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(16.dp),
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        if (isVideo) {
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.6f),
                                                                shape = RoundedCornerShape(bottomStart = 4.dp),
                                                                modifier = Modifier
                                                                    .align(Alignment.TopStart)
                                                                    .padding(2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "MP4",
                                                                    color = Color.White,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 4.dp,
                                                                        vertical = 2.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 删除确认对话框
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showDeleteDialog = false
                                },
                                title = {
                                    Text("确认删除")
                                },
                                text = {
                                    Text("确定要删除选中的 ${selectedImages.size} 个文件吗？此操作不可恢复。")
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false
                                            Toast.makeText(context, "正在删除...", Toast.LENGTH_SHORT).show()

                                            val filesToDelete = selectedImages.mapNotNull { path ->
                                                folderContent?.files?.find { it.file_url == path || it.path == path }
                                            }


                                            filesToDelete.forEach { file ->
                                                val url = "${ServerConfig.baseUrl}${file.file_url ?: file.path}"
                                                val index = imageList.indexOf(url)
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
                                                // 清理状态
                                                selectedImages.clear()
                                                multiSelectMode = false
                                                Toast.makeText(context, "删除完成", Toast.LENGTH_SHORT).show()
                                                refreshFolder()
                                            }
                                        }
                                    ) {
                                        Text("删除", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false
                                            multiSelectMode = false
                                        }
                                    ) {
                                        Text("取消")
                                    }
                                }
                            )
                        }

                        // 预览大图使用同样的排序
                        if (previewImagePath != null) {
                            var isImageListReady by remember { mutableStateOf(false) }
                            val filteredFiles = sortedFiles.filter {
                                it.file_url?.matches(Regex(".*\\.(png|jpg|jpeg|gif|mp4|bmp)$", RegexOption.IGNORE_CASE)) == true
                            }

                            LaunchedEffect(filteredFiles) {
                                imageList.clear()
                                thumbList.clear()
                                fileList.clear()

                                imageList.addAll(filteredFiles.map { "${ServerConfig.baseUrl}${it.file_url}" })
                                thumbList.addAll(filteredFiles.map { "${ServerConfig.baseUrl}${it.thumbnail_url}" })
                                fileList.addAll(filteredFiles.map { it.path })

                                if (currentImageIndex.value >= imageList.size) {
                                    currentImageIndex.value = (imageList.size - 1).coerceAtLeast(0)
                                } else {
                                    currentImageIndex.value = imageList.indexOf(previewImagePath).coerceAtLeast(0)
                                }
                                isImageListReady = true
                            }

                            if (previewImagePath != null && isImageListReady && imageList.isNotEmpty()) {
                                ImageDetailScreen(
                                    imagePaths = imageList,
                                    filePaths = fileList,
                                    thumbPaths = thumbList,
                                    initialIndex = currentImageIndex.value,
                                    onClose = {
                                        previewImagePath = null
                                        isImageListReady = false
                                    },
                                    onGenerateClick = { selectedPath ->
                                        val file = folderContent?.files?.find { "${ServerConfig.baseUrl}${it.file_url}" == selectedPath }
                                        if (file != null && !file.is_dir) {
                                            if (!multiSelectMode) {
                                                // 当前未多选，第一次点击，进入多选模式
                                                selectedImages.clear()
                                                selectedImages.add(file.file_url ?: file.path)
                                                multiSelectMode = true
                                            } else {
                                                // 当前已经多选，第二次点击，退出多选模式
                                                selectedImages.clear()
                                                multiSelectMode = false
                                            }
                                        } else {

                                            Toast.makeText(context, "无法生成：未找到文件或文件夹", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSelectedFileChange = { path ->
                                        // 更新当前右下角弹窗或上下文菜单的文件
                                        selectedFileForMenu = folderContent?.files?.find { "${ServerConfig.baseUrl}${it.file_url}" == path }

                                        // 如果处于多选模式，也可以同步更新多选列表
                                        val file = folderContent?.files?.find { "${ServerConfig.baseUrl}${it.file_url}" == path }
                                        if (multiSelectMode && file != null && !file.is_dir) {
                                            val fullPath = file.file_url ?: file.path
                                            if (!selectedImages.contains(fullPath)) selectedImages.add(fullPath)
                                        }
                                    }
                                )

                            }
                        }

                    }
                }
            }

            if (previewImagePath == null) {
                // 🔹 底部路径切换条
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xFFECECEC).copy(alpha = 0.9f))
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pathOptions.forEach { (displayName, defaultPath) ->
                            val isSelected = currentTab == displayName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(multiSelectMode, currentTab) {
                                        if (!multiSelectMode) {
                                            detectTapGestures(
                                                onTap = {
                                                    // 单击切换 Tab + 刷新
                                                    savePath(currentTab, currentPath)
                                                    currentTab = displayName
                                                    rememberDirectory(
                                                        currentPath,
                                                        getSavedPath(displayName, defaultPath)
                                                    )
                                                    currentPath =
                                                        getSavedPath(displayName, defaultPath)
                                                    scope.launch {
                                                        refreshFolder()
                                                    }
                                                },
                                                onDoubleTap = {
                                                    // 双击滚动到顶部
                                                    scope.launch {
                                                        gridState.animateScrollToItem(0)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    .fillMaxSize()
                            ) {
                                Text(
                                    text = displayName,
                                    color = if (isSelected) Color(0xFF0066FF) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    textAlign = TextAlign.Center, // 🔹 文字水平居中
                                    modifier = Modifier.fillMaxWidth() // 🔹 文字宽度占满整个 Tab
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .fillMaxWidth(0.4f) // 🔹 指示条略小于文字宽度，可调整
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isSelected) Color(0xFF0066FF) else Color.Transparent)
                                )
                            }

                        }
                    }
                }
            }

        }
    }

    if (showGenerateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGenerateSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ImageGenerateBottomSheet(
                imageUrls = generateImageUrls,
                thumbnailUrls = generateThumbnailUrls,
                onDismiss = {
                    showGenerateSheet = false
                    multiSelectMode = false
                    selectedImages.clear()
                }
            )
        }
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            EditImageSheet(
                imageUrls = generateImageUrls,
                thumbnailUrls = generateThumbnailUrls
            ) {
                showEditSheet = false
            }
        }
    }

    // 删除确认弹窗
    if (confirmDeleteDialogVisible && pendingDeleteFile != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialogVisible = false; pendingDeleteFile = null },
            title = { Text("确认删除？") },
            text = { Text("确定要删除这张图片吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteFile?.let { file ->
                        scope.launch {
                            try {
                                RetrofitClient.getApi().deleteFile(file.path)
                                Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                refreshFolder()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    confirmDeleteDialogVisible = false
                    pendingDeleteFile = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDeleteDialogVisible = false; pendingDeleteFile = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    ProgressDialog(
        visible = progressVisible,
        title = "正在上传",
        fileName = currentFileName,
        currentIndex = currentIndex,
        totalCount = totalCount,
    )


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

    if (showNudeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNudeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                    Button(
                        onClick = {
                            showNudeSheet = false
                            scope.launch {
                                performNudeGeneration(
                                    context = context,
                                    selectedImages = selectedImages,
                                    folderContent = folderContent,
                                    refreshFolder = { scope.launch {
                                        refreshFolder()
                                    } },
                                    clearSelection = {
                                        selectedImages.clear()
                                        multiSelectMode = false
                                    },
                                    creativeMode = false
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("蒙版模式")
                    }

                    Button(
                        onClick = {
                            showNudeSheet = false
                            scope.launch {
                                performNudeGeneration(
                                    context = context,
                                    selectedImages = selectedImages,
                                    folderContent = folderContent,
                                    refreshFolder = { scope.launch {
                                        refreshFolder()
                                    } },
                                    clearSelection = {
                                        selectedImages.clear()
                                        multiSelectMode = false
                                    },
                                    creativeMode = true
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffb3424a)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("创意模式")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

    }


    if (multiSelectMode && selectedImages.isNotEmpty()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xFFECECEC).copy(alpha = 0.9f),
                shadowElevation = 32.dp,     // 提升阴影
                tonalElevation = 8.dp       // 细腻分层
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- 换衣 ---
                    IconActionButton(
                        iconPainter = painterResource(id = R.drawable.clothes),
                        tint = Color.Black,
                        label = "换衣",
                        contentDescription = "换衣",
                        iconSize = 25.dp
                    ) {
                        if (selectedImages.isNotEmpty()) {
                            showNudeSheet = true
                        } else {
                            Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // --- 修图 ---
                    IconActionButton(
                        iconPainter = painterResource(id = R.drawable.picture),
                        tint = Color.Black,
                        label = "修图",
                        contentDescription = "修图"
                    ) {
                        if (selectedImages.isNotEmpty()) {
                            generateImageUrls = selectedImages.mapNotNull { path ->
                                folderContent?.files?.find { it.file_url == path || it.path == path }?.file_url
                            }
                            generateThumbnailUrls = selectedImages.mapNotNull { path ->
                                folderContent?.files?.find { it.file_url == path || it.path == path }?.thumbnail_url
                            }

                            showEditSheet = true  // ✅ 弹出修图界面
                        } else {
                            Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                        }
                        multiSelectMode = false
                    }

                    // --- 动图 ---
                    IconActionButton(
                        iconPainter = painterResource(id = R.drawable.video),
                        tint = Color.Black,
                        label = "动图",
                        contentDescription = "动图",
                        iconSize = 28.dp
                    ) {
                        if (selectedImages.isNotEmpty()) {
                            generateImageUrls = selectedImages.mapNotNull { path ->
                                folderContent?.files?.find { it.file_url == path || it.path == path }?.file_url
                            }
                            generateThumbnailUrls = selectedImages.mapNotNull { path ->
                                folderContent?.files?.find { it.file_url == path || it.path == path }?.thumbnail_url
                            }

                            showGenerateSheet = true
                        } else {
                            Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                        }
                        multiSelectMode = false
                    }

                    // --- 剪切 / 粘贴 ---
                    IconActionButton(
                        iconPainter = painterResource(
                            id = if (cutList.isEmpty()) R.drawable.cut else R.drawable.paste
                        ),
                        tint = Color.Black,
                        label = if (cutList.isEmpty()) "剪切" else "粘贴",
                        contentDescription = if (cutList.isEmpty()) "剪切" else "粘贴",
                        iconSize = 26.dp
                    ) {
                        if (cutList.isEmpty()) {
                            // ---------------------------------------
                            //             执行“剪切”
                            // ---------------------------------------
                            if (selectedImages.isEmpty()) {
                                Toast.makeText(context, "未选择任何图片", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            cutList = selectedImages.toList()
                            cutSourceDir = currentPath

                            Toast.makeText(context, "已剪切 ${cutList.size} 项", Toast.LENGTH_SHORT).show()
                            selectedImages.clear()
                            multiSelectMode = false

                        } else {
                            // ---------------------------------------
                            //             执行“粘贴”
                            // ---------------------------------------
                            val targetDir = currentPath

                            if (targetDir == cutSourceDir) {
                                Toast.makeText(context, "目标文件夹与原位置相同", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            scope.launch {
                                cutList.forEach { fileUrl ->

                                    val file = folderContent?.files?.find {
                                        it.file_url == fileUrl || it.path == fileUrl
                                    }

                                    file?.let { f ->
                                        try {
                                            val src = f.path ?: f.file_url!!
                                            val dest = targetDir

                                            RetrofitClient.getApi().moveFile(src, dest)

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "移动失败: ${f.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                // 清空剪切板
                                cutList = emptyList()
                                cutSourceDir = ""

                                Toast.makeText(context, "已完成移动", Toast.LENGTH_SHORT).show()

                                // 刷新当前文件夹
                                refreshFolder()
                            }
                        }
                    }

                    // --- 下载 ---
                    // 在 AlbumScreen 内
                    var downloadDialogVisible by remember { mutableStateOf(false) }
                    var currentDownloadingFile by remember { mutableStateOf("") }
                    var currentIndex by remember { mutableStateOf(0) }
                    val totalCount = selectedImages.size

                    IconActionButton(
                        iconPainter = painterResource(id = R.drawable.download),
                        tint = Color.Black,
                        label = "下载",
                        contentDescription = "下载",
                        iconSize = 25.dp
                    ) {
                        if (selectedImages.isNotEmpty()) {
                            scope.launch {
                                downloadDialogVisible = true
                                currentIndex = 0
                                selectedImages.forEachIndexed { index, imagePath ->
                                    currentIndex = index + 1
                                    currentDownloadingFile = imagePath.substringAfterLast("/")
                                    try {
                                        val fullUrl = "${ServerConfig.baseUrl}$imagePath"
                                        val filename = imagePath.substringAfterLast("/")

                                        withContext(Dispatchers.IO) {
                                            val request = okhttp3.Request.Builder().url(fullUrl).build()
                                            val response = okhttp3.OkHttpClient().newCall(request).execute()
                                            if (!response.isSuccessful) throw Exception("下载失败")

                                            response.body?.byteStream()?.use { inputStream ->
                                                val savedUri = saveFileToGallery(context, inputStream, filename)
                                                withContext(Dispatchers.Main) {
                                                    if (savedUri != null) {
//                                                        Toast.makeText(context, "已保存：$filename", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "保存失败：$filename", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            response.close()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "下载出错: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                downloadDialogVisible = false
                                multiSelectMode = false
                            }
                        } else {
                            Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                        }
                    }

                    ProgressDialog(
                        visible = downloadDialogVisible,
                        title = "正在下载",
                        fileName = currentDownloadingFile,
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                    )

                    // --- 删除 ---
                    if (cutList.isEmpty()){
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.delete),
                            tint = Color.Black,
                            label = "删除",
                            contentDescription = "删除"
                        ) {
                            if (selectedImages.isNotEmpty()) {
                                showDeleteDialog = true
                            } else {
                                Toast.makeText(context, "没有可删除的文件", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.delete),
                            tint = Color.Black,
                            label = "清空",
                            contentDescription = "清空"
                        ) {
                            // 清空剪切板
                            cutList = emptyList()
                            cutSourceDir = ""

                            Toast.makeText(context, "已清空", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

                    // 新增图片
                    Button(
                        onClick = {
                            showAddSheet = false
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("新增图片")
                    }

                    // 新增文件夹
                    Button(
                        onClick = {
                            showAddSheet = false
                            showCreateFolderDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffb3424a)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("新增文件夹")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            showCreateFolderDialog = false

                            scope.launch {
                                try {
                                    RetrofitClient.getApi().createFolder(
                                        parent = currentPath,
                                        name = folderName
                                    )

                                    Toast.makeText(context, "文件夹已创建", Toast.LENGTH_SHORT).show()

                                    // 刷新文件夹
                                    refreshFolder()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "创建失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("取消")
                }
            }
        )
    }


}


@Composable
fun IconActionButton(
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    tint: Color = Color.Unspecified,
    label: String,
    contentDescription: String = label,
    iconSize: Dp = 28.dp,
    iconBoxHeight: Dp = 32.dp, // ✅ 固定图标区域高度
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // ✅ 固定图标区域，不随图标大小改变
        Box(
            modifier = Modifier
                .height(iconBoxHeight),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconVector != null -> {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = contentDescription,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                }
                iconPainter != null -> {
                    Icon(
                        painter = iconPainter,
                        contentDescription = contentDescription,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        Text(
            text = label,
            color = tint,
            fontSize = 12.sp
        )
    }
}


@Composable
fun GridWithVerticalScrollHandleOverlay(
    allItems: List<FileInfo>,
    columns: Int = 3,
    gridState: LazyGridState,
    handleHeight: Dp = 40.dp,       // 滑块高度
    handleWidth: Dp = 28.dp,        // 滑块宽度
    trackPaddingTop: Dp = 120.dp,    // 轨道顶部 padding
    trackPaddingBottom: Dp = 96.dp, // 轨道底部 padding
    content: @Composable (LazyGridState) -> Unit
) {
    if (allItems.isEmpty()) return

    val scope = rememberCoroutineScope()
    var handleOffset by remember { mutableStateOf(0f) }
    var trackHeightPx by remember { mutableStateOf(0f) }
    var gridWidthPx by remember { mutableStateOf(0f) }
    val handleHeightPx = with(LocalDensity.current) { handleHeight.toPx() }
    val paddingTopPx = with(LocalDensity.current) { trackPaddingTop.toPx() }
    val paddingBottomPx = with(LocalDensity.current) { trackPaddingBottom.toPx() }
    var isDragging by remember { mutableStateOf(false) }
    var showHandle by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ------------------ Grid 内容 ------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    gridWidthPx = coords.size.width.toFloat()
                }
        ) {
            content(gridState)
        }

        // ------------------ 滑块轨道 ------------------
        Box(
            modifier = Modifier
                .width(handleWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coords ->
                    trackHeightPx = coords.size.height.toFloat() - paddingTopPx - paddingBottomPx
                }
        ) {
            if (showHandle) {
                val dragState = rememberDraggableState { delta ->
                    isDragging = true
                    handleOffset = (handleOffset + delta)
                        .coerceIn(0f, (trackHeightPx - handleHeightPx).coerceAtLeast(0f))

                    val rowHeightPx = gridWidthPx / columns
                    val totalRows = ceil(allItems.size / columns.toFloat())
                    val totalHeightPx = totalRows * rowHeightPx

                    val scrollY = (handleOffset / (trackHeightPx - handleHeightPx)) * (totalHeightPx - trackHeightPx)

                    val targetRowF = scrollY / rowHeightPx
                    val targetRow = targetRowF.toInt().coerceIn(0, totalRows.toInt() - 1)

                    val rowOffset = ((targetRowF - targetRow) * rowHeightPx).toInt()

                    val targetIndex = targetRow * columns

                    scope.launch {
                        gridState.scrollToItem(targetIndex, rowOffset)
                    }

                }


                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (handleOffset + paddingTopPx).roundToInt()) }
                        .width(handleWidth)
                        .height(handleHeight)
                        .shadow(
                            elevation = 8.dp, // 阴影高度，可调
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                            clip = false
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = dragState,
                            onDragStopped = { isDragging = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sort),
                        contentDescription = "滑块",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ------------------ 同步 Grid 滚动 ------------------
            LaunchedEffect(
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset,
                gridWidthPx,
                trackHeightPx
            ) {
                if (gridWidthPx > 0f && trackHeightPx > 0f) {
                    val rowHeightPx = gridWidthPx / columns
                    val totalRows = ceil(allItems.size / columns.toFloat())
                    val totalHeightPx = totalRows * rowHeightPx

                    // 内容不足3屏时隐藏滑块
                    showHandle = totalHeightPx > 3 * trackHeightPx

                    if (showHandle) {
                        if (!isDragging) {
                            val scrollY =
                                (gridState.firstVisibleItemIndex / columns) * rowHeightPx +
                                        gridState.firstVisibleItemScrollOffset.toFloat()
                            handleOffset =
                                (scrollY / (totalHeightPx - trackHeightPx)) * (trackHeightPx - handleHeightPx)
                        }
                    } else {
                        handleOffset = 0f
                    }
                }
            }
        }
    }
}


@Composable
fun ProgressDialog(
    visible: Boolean,
    title: String,
    fileName: String,
    currentIndex: Int,
    totalCount: Int,
) {
    if (!visible) return
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFFFFF),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = fileName,
                            color = Color.Black,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "进度: $currentIndex / $totalCount",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
