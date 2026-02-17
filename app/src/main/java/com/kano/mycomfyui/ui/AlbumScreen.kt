package com.kano.mycomfyui.ui

import VideoDetailScreen
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.text.Collator
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun AlbumScreen(
    onExitApp: () -> Unit,
    navController: NavHostController,
    onLockClick: () -> Unit,
) {

    /**
     * 变量区
     */
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedFileForMenu by remember { mutableStateOf<FileInfo?>(null) }
    var confirmDeleteDialogVisible by remember { mutableStateOf(false) }
    var pendingDeleteFile by remember { mutableStateOf<FileInfo?>(null) }
    var showGenerateSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val gson = Gson()
    val prefs: SharedPreferences = context.getSharedPreferences("album_cache", Context.MODE_PRIVATE)
    val prefs1: SharedPreferences = context.getSharedPreferences("path_cache", Context.MODE_PRIVATE)
    var multiSelectMode by remember { mutableStateOf(false) }

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
    val scrollPositions = remember {
        mutableStateMapOf<String, Pair<Int, Int>>()
    }
    var progressVisible by remember { mutableStateOf(false) }
    var currentFileName by remember { mutableStateOf("") }
    var uploadIndex by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }

    val refreshState = rememberPullToRefreshState()

    var showTextInputDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isTopBarVisible by remember { mutableStateOf(true) }

    var overlayVisible by remember { mutableStateOf(true) }

    val bottomBarAlpha by animateFloatAsState(
        targetValue = if (overlayVisible) 0f else 1f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "BottomBarAlpha"
    )

    val bringIntoViewRequesters =
        remember { mutableStateMapOf<String, BringIntoViewRequester>() }

    val pathOptions = buildList {
        add("修图" to "修图")
        add("素材" to "素材")
        add("动图" to "动图")
    }

    val hideStates = remember { mutableStateMapOf<String, Boolean>() }
    var imageClosing by remember { mutableStateOf(true) }


    val viewModel: FolderViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState() // ViewModel状态
    val folderContent = uiState.folderContent

    var useDarkTopBar by remember { mutableStateOf(false) }
    val topBarColor = if (useDarkTopBar) Color.White else Color.Black
    var clickedThumbBounds by remember { mutableStateOf<ImageBounds?>(null) }
    val visibleCoordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() } // 可见图片位置

    val modePrefs = context.getSharedPreferences("mode_cache", Context.MODE_PRIVATE)

    var fileMode by remember {
        mutableStateOf(
            Mode.fromValue(
                modePrefs.getString("file_mode", Mode.ALL.value)
            )
        )
    }

    var imageColumns by remember {
        mutableIntStateOf(
            modePrefs.getInt("image_columns", 3)
        )
    }

    var sortMode by remember {
        mutableStateOf(
            modePrefs.getString("sortMode", "从旧到新")
        )
    }

    var folderMode by remember {
        mutableStateOf(
            modePrefs.getString("folderMode", "按名称")
        )
    }

    var copyOrCut by remember { mutableStateOf("") }

    var showCutDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    var showPerspective by remember { mutableStateOf(false) }
    var perspectiveFiles by remember { mutableStateOf<List<FileInfo>?>(null) }

    var isLoading by remember { mutableStateOf(false) }


    data class CachedFolder(
        val content: FolderContent,
        val timestamp: Long
    )

    val CACHE_EXPIRE_TIME = 30 * 60 * 1000L

    /**
     * 函数区
     */
    fun sendTextToGenerate(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.getApi().generateImage(
                    type = "生图",
                    imageUrl = "",
                    thumbnailUrl = "",
                    args = mapOf("text" to text)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun saveFolderCache(path: String, content: FolderContent) {
        val cached = CachedFolder(
            content = content,
            timestamp = System.currentTimeMillis()
        )
        val json = gson.toJson(cached)
        prefs.edit { putString(path, json) }
    }

    fun getFolderCache(path: String): FolderContent? {
        val json = prefs.getString(path, null) ?: return null

        return try {
            val cached = gson.fromJson(json, CachedFolder::class.java)

            val isExpired =
                System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRE_TIME

            if (isExpired) {
                // 过期自动删除
                prefs.edit { remove(path) }
                null
            } else {
                cached.content
            }
        } catch (e: Exception) {
            // 结构变更导致反序列化失败时清掉
            prefs.edit { remove(path) }
            null
        }
    }

    suspend fun updateCacheSilently(path: String) {
        try {
            val serverContent = RetrofitClient.getApi().browse(path)
            saveFolderCache(path, serverContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePath(key: String, path: String) {
        prefs1.edit { putString(key, path) }
    }

    fun getSavedPath(key: String, defaultPath: String): String {
        return prefs1.getString(key, defaultPath) ?: defaultPath
    }

    // 拉取 API
    suspend fun refreshFolder(requestedPath: String) {
        savePath(currentTab, requestedPath)

        // 1️⃣ 本地缓存
        getFolderCache(requestedPath)?.let { cached ->

            viewModel.updateFolderContent(
                content = cached,
                currentPath = requestedPath,
                mode = FolderViewModel.ContentUpdateMode.REFRESH,
                fileMode = fileMode,
                sortMode = sortMode.toString()
            )
        }

        try {
            val serverContent =
                RetrofitClient.getApi().browse(requestedPath)

            // 2️⃣ 只有路径没变才更新
            if (requestedPath == viewModel.uiState.value.currentPath) {
                viewModel.updateFolderContent(
                    content = serverContent,
                    currentPath = requestedPath,
                    mode = FolderViewModel.ContentUpdateMode.REFRESH,
                    fileMode = fileMode,
                    sortMode = sortMode.toString()

                )

                saveFolderCache(requestedPath, serverContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (requestedPath == viewModel.uiState.value.currentPath) {
                Toast.makeText(context, "刷新失败", Toast.LENGTH_SHORT).show()
            }
        }
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
            val pathBody = uiState.currentPath.toRequestBody("text/plain".toMediaType())

            // 发起上传请求并解析响应
            val response = RetrofitClient.getApi().uploadImage(pathBody, body)

            if (!response.isSuccessful) {
                Toast.makeText(context, "上传失败：${response.code()}", Toast.LENGTH_SHORT).show()
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

    suspend fun uploadImagesAndRefresh(
        uris: List<Uri>
    ) {
        if (uris.isEmpty()) return
        uploadIndex = 1
        totalCount = uris.size
        progressVisible = true

        try {
            uris.asReversed().forEach { uri ->
                currentFileName =
                    uri.lastPathSegment ?: "image"
                uploadImageFromUri(uri)
                uploadIndex += 1
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "上传失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            progressVisible = false
            scope.launch {
                refreshFolder(uiState.currentPath)
            }

        }
    }

    fun resolveTabPath(
        tabKey: String,
        defaultPath: String
    ): String {
        val savedPath = prefs1.getString(tabKey, null)

        return when {
            savedPath.isNullOrBlank() -> defaultPath
            !savedPath.startsWith(defaultPath) -> defaultPath
            else -> savedPath
        }
    }


    suspend fun switchTab(
        newTab: String,
        defaultPath: String,
        uiState: ImageViewerState
    ) {
        // 1️⃣ 保存旧 Tab 的路径
        savePath(currentTab, uiState.currentPath)

        // 2️⃣ 更新当前 Tab
        currentTab = newTab

        // 3️⃣ 解析目标路径（唯一规则）
        val targetPath = resolveTabPath(newTab, defaultPath)

        // 4️⃣ 记忆目录滚动
        rememberDirectory(uiState.currentPath, targetPath)

        // 5️⃣ 更新路径
        viewModel.updateCurrentPath(targetPath)

        // 6️⃣ 刷新
        refreshFolder(targetPath)
    }


    /**
     * 变量区
     */
    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->

            if (uris.isEmpty()) return@rememberLauncherForActivityResult

            scope.launch {
                uploadImagesAndRefresh(uris)
            }
        }

    /**
     * 副作用区
     */
    DisposableEffect(multiSelectMode) {
        onDispose {
            if (!multiSelectMode) {
                viewModel.clearSelection() //取消多选模式清空选中图片
            }
        }
    }

    // 监测生命周期，在程序结束前保存目录状态
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_PAUSE ||
                event == Lifecycle.Event.ON_STOP ||
                event == Lifecycle.Event.ON_DESTROY
            ) {
                val path = viewModel.uiState.value.currentPath
                if (path.isNotBlank()) {
                    rememberDirectory(path, path)
                }
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)

        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentTab) {
        val initialPath = getSavedPath(
            currentTab,
            pathOptions.first { it.first == currentTab }.second
        )

        viewModel.setCurrentPath(initialPath) // 初始化目录
        scope.launch {
            refreshFolder(uiState.currentPath)
        }

    }

    LaunchedEffect(uiState.currentPath, uiState.sortedFiles.size) {
        val pos = scrollPositions[uiState.currentPath]
        if (pos != null) {
            gridState.scrollToItem(pos.first, pos.second) // 恢复记忆位置
        }
    }

    LaunchedEffect(fileMode) {
        modePrefs.edit {
            putString("file_mode", fileMode.value)
        }
    }

    BackHandler(enabled = true) {
        when {
            multiSelectMode -> {
                viewModel.clearSelection()
                multiSelectMode = false
                showMoreSheet = false
            }

            showPerspective == true -> {
                showPerspective = false
                perspectiveFiles = null
                isTopBarVisible = !isTopBarVisible
                showMoreSheet = false
            }

            uiState.previewPath != null -> {
                viewModel.closePreview()
                viewModel.clearSelection()
                isTopBarVisible = true
                showMoreSheet = false
            }

            uiState.previewVideo != null -> {
                viewModel.closePreviewVideo()
                isTopBarVisible = true
                showMoreSheet = false
            }

            // 3️⃣ 返回父目录
            currentTab != "最新" &&
                    uiState.currentPath !in listOf("素材", "动图", "修图", "生图") &&
                    uiState.folderContent?.parent != null -> {

                val parentPath = uiState.folderContent!!.parent.path

                // 记住当前目录滚动位置
                rememberDirectory(uiState.currentPath, parentPath)

                viewModel.updateCurrentPath(parentPath)

                scope.launch {
                    refreshFolder(uiState.currentPath)
                }

            }

            // 4️⃣ 退出应用
            else -> onExitApp()
        }
    }

    /**
     * UI区
     */
    Scaffold(
        modifier = Modifier.background(Color.White),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (true){
                val pathParts = uiState.currentPath
                    .split("/")
                    .filter { it.isNotBlank() }

                var topText = pathParts.lastOrNull() ?: ""

                var bottomText = pathParts
                    .dropLast(1)
                    .joinToString("·")

                while (bottomText.length > 30 && bottomText.contains("·")) {
                    bottomText = bottomText.substringAfter("·")
                }


                if (isTopBarVisible) {
                    TopAppBar(
                        title = {
                            Column {
                                if(topText.isNotEmpty()){
                                    Text(
                                        text = topText,
                                        color = topBarColor,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (bottomText.isNotEmpty()){
                                    Text(
                                        text = bottomText,
                                        color = topBarColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                            }

                        },
                        actions = {
                            var currentToast by remember { mutableStateOf<Toast?>(null) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(0.dp) // 👈 控制间距
                            ) {
                                var expanded by remember { mutableStateOf(false) }

                                var expanded1 by remember { mutableStateOf(false) }  // 控制菜单展开状态

                                if (multiSelectMode) {
                                    IconButton(
                                        onClick = {

                                            val allFiles = folderContent?.files.orEmpty()
                                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            // ✅ 当前模式下可见文件
                                            val visibleFiles = sortPreviewableFiles(
                                                files = allFiles,
                                                currentPath = uiState.currentPath,
                                                dateFormat = dateFormat,
                                                mode = fileMode,
                                                sortMode = sortMode.toString()
                                            )

                                            val selectableCount = visibleFiles.size

                                            if (uiState.selectedPaths.size == selectableCount) {
                                                // 已全选 → 清空
                                                viewModel.clearSelection()
                                            } else {
                                                // 未全选 → 只选可见文件
                                                viewModel.selectAllFiles(visibleFiles)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.check),
                                            contentDescription = "全选",
                                            tint = topBarColor,
                                            modifier = Modifier.height(18.dp)
                                        )
                                    }

                                } else {
                                    IconButton(onClick = {
                                        showAddSheet = true
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.upload),
                                            contentDescription = "新增",
                                            tint = topBarColor,
                                            modifier = Modifier.height(25.dp).offset(x = (-3).dp, y = 0.dp),
                                        )
                                    }
                                }

                                IconButton(onClick = { expanded1 = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.file_mode),
                                        contentDescription = "切换模式",
                                        tint = topBarColor,
                                        modifier = Modifier.height(22.dp)
                                    )
                                    DropdownMenu(
                                        expanded = expanded1,
                                        onDismissRequest = { expanded1 = false },
                                        modifier = Modifier
                                            .width(240.dp) // 控制整体宽度
                                            .background(Color.White)
                                            .padding(horizontal = 18.dp, vertical = 4.dp), // 紧凑一点的内边距
                                        offset = DpOffset(x = (48.dp), y = 0.dp) // 负的 x 偏移贴右

                                    ) {
                                        Column(modifier = Modifier.padding(4.dp)) {
                                            // 第一组：过滤模式
                                            Text("过滤模式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            val modes = listOf(
                                                Mode.ALL to "全部",
                                                Mode.ORIGIN to "原始",
                                                Mode.NUDE to "脱衣",
                                                Mode.EDIT to "修图",
                                                Mode.VIDEO to "视频"
                                            )

                                            // 每行最多三个标签
                                            modes.chunked(3).forEach { rowModes ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    rowModes.forEach { (mode, label) ->
                                                        Button(
                                                            onClick = {
                                                                fileMode = mode
                                                                expanded1 = false

                                                                currentToast?.cancel()
                                                                currentToast = Toast.makeText(context, "当前模式: $label", Toast.LENGTH_SHORT)
                                                                currentToast?.show()

                                                                scope.launch { refreshFolder(uiState.currentPath) }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (fileMode == mode) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                                contentColor = if (fileMode == mode) Color.White else Color.Black
                                                            ),
                                                            shape = RoundedCornerShape(10.dp), // ✅ 设置圆角大小
                                                            modifier = Modifier
                                                                .wrapContentHeight()
                                                                .height(28.dp), // 紧凑高度
                                                            contentPadding = PaddingValues(vertical = 0.dp)
                                                        ) {
                                                            Text(label, fontSize = 12.sp)
                                                        }
                                                    }

                                                    if (rowModes.size < 3) {
                                                        repeat(3 - rowModes.size) { Spacer(modifier = Modifier.weight(1f)) }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // 第二组：图片列数
                                            Text("图片列数", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            val cols = listOf(2, 3, 4)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                cols.forEach { col ->
                                                    Button(
                                                        onClick = {
                                                            imageColumns = col
                                                            modePrefs.edit {
                                                                putInt(
                                                                    "image_columns",
                                                                    col
                                                                )
                                                            }
                                                            expanded1 = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (imageColumns == col) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            contentColor = if (imageColumns == col) Color.White else Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(10.dp), // ✅ 设置圆角大小
                                                        modifier = Modifier
                                                            .wrapContentHeight()
                                                            .height(28.dp),
                                                        contentPadding = PaddingValues(vertical = 0.dp)
                                                    ) {
                                                        Text("${col}列", fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // 第二组：图片列数
                                            Text("图片排序方式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            val sortModes = listOf("从旧到新", "从新到旧")

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                sortModes.forEach { sort ->
                                                    Button(
                                                        onClick = {
                                                            sortMode = sort
                                                            modePrefs.edit {
                                                                putString(
                                                                    "sortMode",
                                                                    sort
                                                                )
                                                            }
                                                            scope.launch { refreshFolder(uiState.currentPath) }

                                                            expanded1 = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (sortMode == sort) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            contentColor = if (sortMode == sort) Color.White else Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(10.dp), // ✅ 设置圆角大小
                                                        modifier = Modifier
                                                            .wrapContentHeight()
                                                            .height(28.dp),
                                                        contentPadding = PaddingValues(vertical = 0.dp)
                                                    ) {
                                                        Text(sort, fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text("目录排序方式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            val folderModes = listOf("按时间", "按名称")

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                folderModes.forEach { mode ->
                                                    Button(
                                                        onClick = {
                                                            folderMode = mode
                                                            modePrefs.edit {
                                                                putString(
                                                                    "folderMode",
                                                                    mode
                                                                )
                                                            }
                                                            scope.launch { refreshFolder(uiState.currentPath) }

                                                            expanded1 = false
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (folderMode == mode) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                            contentColor = if (folderMode == mode) Color.White else Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(10.dp), // ✅ 设置圆角大小
                                                        modifier = Modifier
                                                            .wrapContentHeight()
                                                            .height(28.dp),
                                                        contentPadding = PaddingValues(vertical = 0.dp)
                                                    ) {
                                                        Text(mode, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))

                                        }
                                    }
                                }


                                IconButton(onClick = {
                                    expanded = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "更多",
                                        tint = topBarColor,
                                        modifier = Modifier.height(22.dp).offset(y = (-2).dp)
                                    )


                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier
                                            .width(90.dp)
                                            .background(Color.White)
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
                                                    RetrofitClient.getApi().refresh(uiState.currentPath)
                                                    refreshFolder(uiState.currentPath)
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("设置") },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("settings")
                                            }
                                        )
                                    }
                                }


                            }
                        },
                        colors = topAppBarColors(
                            containerColor = Color.White,   // 背景透明
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
            }
        },
        floatingActionButton = {
            if (currentTab == "生图") {
                FloatingActionButton(
                    onClick = {
                        inputText = ""
                        showTextInputDialog = true
                    },
                    containerColor = Color(0xFF2196F3), // 蓝色（Material Blue 500）
                    contentColor = Color.White,
                    modifier = Modifier.offset(y = (-96).dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "生图"
                    )
                }
            }

        }
    ) {
        Box (
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(currentTab, multiSelectMode) {

                    if (!multiSelectMode) { // 多选模式下不响应滑动
                        detectHorizontalDragGestures { change, dragAmount ->
                            val currentIndex = pathOptions.indexOfFirst { it.first == currentTab }
                            scope.launch {
                                val newIndex = when {
                                    dragAmount >80 && currentIndex > 0 -> currentIndex - 1
                                    dragAmount < -80 && currentIndex < pathOptions.size - 1 -> currentIndex + 1
                                    else -> return@launch
                                }

                                val (tabKey, defaultPath) = pathOptions[newIndex]

                                switchTab(
                                    newTab = tabKey,
                                    defaultPath = defaultPath,
                                    uiState = uiState
                                )
                            }

                        }
                    }
                }
        ) {


            Column(modifier = Modifier.fillMaxSize()) {

                Box(modifier = Modifier.weight(1f)) {
                    // 图片/文件夹网格
                    folderContent?.let { content ->

                        val collator = Collator.getInstance(Locale.CHINA)

                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

                        val sortedFolders = when (folderMode) {
                            "按时间" -> content.folders.sortedWith { a, b ->
                                try {
                                    val timeA = a.updated_at?.let { LocalDateTime.parse(it, formatter) }
                                    val timeB = b.updated_at?.let { LocalDateTime.parse(it, formatter) }

                                    // 如果时间为空，就用极小时间代替，确保不会 NPE
                                    val safeTimeA = timeA ?: LocalDateTime.MIN
                                    val safeTimeB = timeB ?: LocalDateTime.MIN

                                    val cmp = safeTimeB.compareTo(safeTimeA) // 降序
                                    if (cmp != 0) cmp else collator.compare(a.name, b.name)
                                } catch (e: Exception) {
                                    0 // 出错就认为相等，不影响排序
                                }
                            }
                            else -> content.folders.sortedWith { a, b ->
                                collator.compare(a.name, b.name)
                            }
                        }

                        val allItems = sortedFolders + uiState.sortedFiles
                        val fileCoordsMap = remember { mutableStateMapOf<String, LayoutCoordinates>() }

                        if (readyToDisplay) {

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true

                                        val startTime = System.currentTimeMillis()

                                        // 执行实际刷新逻辑
                                        rememberDirectory(uiState.currentPath, uiState.currentPath)

                                        refreshFolder(uiState.currentPath)


                                        // 计算已用时间
                                        val elapsed = System.currentTimeMillis() - startTime
                                        val minDuration = 180L

                                        if (elapsed < minDuration) {
                                            delay(minDuration - elapsed) // 等待剩余时间
                                        }

                                        isRefreshing = false
                                    }
                                },
                                state = refreshState,
                                indicator = {
                                    Indicator(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 100.dp),
                                        isRefreshing = isRefreshing,
                                        containerColor = Color.White,
                                        color = Color(0xFF0066FF),
                                        state = refreshState,
                                    )
                                },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                GridWithVerticalScrollHandleOverlay(
                                    modifier = Modifier,
                                    allItems = allItems,
                                    columns = imageColumns,
                                    handleHeight = 40.dp,
                                    gridState = gridState,
                                    gridPaddingTop = 108.dp,
                                    gridPaddingBottom = 64.dp
                                ) {
                                    if (allItems.isEmpty()) {
                                        // 空状态全屏显示
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(top = 108.dp, bottom = 64.dp), // 保持和 Grid 一样的 padding
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "无文件夹或媒体文件",
                                                fontSize = 16.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    fun getItemIndexAndFileByPreviewPath(path: String?): Pair<Int, FileInfo?> {
                                        if (path == null) return -1 to null
                                        val index = allItems.indexOfFirst { file -> path == file.net_url }
                                        val file = if (index >= 0) allItems[index] else null
                                        return index to file
                                    }


                                    fun isItemVisible(gridState: LazyGridState, index: Int): Boolean {
                                        if (index == -1) return false
                                        val visibleItems = gridState.layoutInfo.visibleItemsInfo
                                        val firstVisible = visibleItems.firstOrNull()?.index ?: -1
                                        val lastVisible = visibleItems.lastOrNull()?.index ?: -1
                                        return index in firstVisible..lastVisible
                                    }

                                    LaunchedEffect(uiState.previewPath) {
                                        val path = uiState.previewPath ?: return@LaunchedEffect

                                        val (targetIndex, targetFile) =
                                            getItemIndexAndFileByPreviewPath(path)

                                        if (targetIndex < 0 || targetFile == null) return@LaunchedEffect

                                        // 先 bringIntoView（如果你有）
                                        bringIntoViewRequesters[targetFile.path]?.bringIntoView()

                                        // 再兜底滚动
                                        if (!isItemVisible(gridState, targetIndex)) {
                                            gridState.animateScrollToItem(targetIndex)
                                        }
                                    }


                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Fixed(imageColumns),
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        if (allItems.isEmpty()) {
                                            item {

                                            }
                                        }
                                        items(allItems, key = { it.path }) { file ->
                                            val url = file.file_url?.let { "${ServerConfig.baseUrl}$it" }

                                            val requester = remember { BringIntoViewRequester() }

                                            LaunchedEffect(file.path) {
                                                bringIntoViewRequesters[file.path] = requester
                                            }

                                            if (!hideStates.containsKey(url)) {
                                                hideStates[url.toString()] = false
                                            }

                                            LaunchedEffect(uiState.previewPath) {
                                                val key = url.toString()

                                                if (uiState.previewPath == url) {
                                                    delay(50)
                                                    hideStates[key] = true
                                                } else {
                                                    hideStates[key] = false
                                                }
                                            }

                                            LaunchedEffect(gridState) {
                                                snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                                                    .collect { visibleItems ->
                                                        visibleCoordsMap.clear()
                                                        visibleItems.forEach { itemInfo ->
                                                            val key = itemInfo.key as? String ?: return@forEach
                                                            fileCoordsMap[key]?.let { coords ->
                                                                visibleCoordsMap[key] = coords
                                                            }
                                                        }
                                                    }
                                            }

                                            Box( // ✅ 最外层
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bringIntoViewRequester(requester)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .onGloballyPositioned { coords ->
                                                            fileCoordsMap[file.path] = coords
                                                        }
                                                        .aspectRatio(1f)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (file.is_dir) {
                                                                    // 打开目录
                                                                    rememberDirectory(
                                                                        uiState.currentPath,
                                                                        file.path
                                                                    )
                                                                    viewModel.updateCurrentPath(file.path)
                                                                    scope.launch {
                                                                        refreshFolder(uiState.currentPath)
                                                                    }

                                                                } else {
                                                                    // 文件才使用 URL
                                                                    if (url == null) {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "文件未准备好，请稍候",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                        return@combinedClickable
                                                                    }
                                                                    if (multiSelectMode) {
                                                                        // 多选：只交给 ViewModel
                                                                        viewModel.toggleSelect(file)

                                                                    } else {
                                                                        val isVideo =
                                                                            file.file_url.lowercase()
                                                                                .endsWith(".mp4") == true
                                                                        if (isVideo) {
                                                                            viewModel.openPreviewVideo(
                                                                                filePath = file.net_url.toString()
                                                                            )
                                                                        } else {
                                                                            // 单选 + 打开预览
                                                                            val indexInSortedFiles =
                                                                                uiState.sortedFiles.indexOfFirst { it.path == file.path }

                                                                            if (indexInSortedFiles >= 0) {
                                                                                showMoreSheet = false
                                                                                viewModel.openPreview(
                                                                                    file = file,
                                                                                    index = indexInSortedFiles
                                                                                )
                                                                            }

                                                                            // UI 层还能保留的
                                                                            selectedFileForMenu = file
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                if (!file.is_dir) {
                                                                    multiSelectMode = true
                                                                    viewModel.selectOnly(file)
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
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .onGloballyPositioned { coordinates ->
                                                                    visibleCoordsMap[file.path]?.let { coords ->
                                                                        try {
                                                                            // 检查是否仍然有效
                                                                            if (coords.isAttached) {
                                                                                val pos =
                                                                                    coords.positionInWindow()
                                                                                val size =
                                                                                    coords.size
                                                                                clickedThumbBounds =
                                                                                    ImageBounds(
                                                                                        left = pos.x,
                                                                                        top = pos.y,
                                                                                        width = size.width.toFloat(),
                                                                                        height = size.height.toFloat()
                                                                                    )
                                                                            } else {
                                                                                // 清理无效的坐标
                                                                                visibleCoordsMap.remove(
                                                                                    file.path
                                                                                )
                                                                                clickedThumbBounds =
                                                                                    null
                                                                            }
                                                                        } catch (e: IllegalStateException) {
//                                                                            Log.e("debug", e.message.toString())
                                                                            // 捕获异常并清理
                                                                            visibleCoordsMap.remove(
                                                                                file.path
                                                                            )
                                                                            clickedThumbBounds =
                                                                                null
                                                                        }
                                                                    }
                                                                }
                                                        ) {
                                                            val path = file.file_url

                                                            hideStates[url.toString()]?.let { it1 ->
                                                                if (!it1){
                                                                    AsyncImage(
                                                                        model = ImageRequest.Builder(context)
                                                                            .data(file.thumb_url ?: file.net_url)
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
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    0.dp
                                                                                )
                                                                            )
                                                                            .background(Color.LightGray)
                                                                    )
                                                                }
                                                            }
                                                            val pathIsSelect = uiState.selectedPaths.contains(path)

                                                            if (pathIsSelect) {
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
                                                                            shape = RoundedCornerShape(
                                                                                2.dp
                                                                            )
                                                                        )
                                                                        .background(
                                                                            color = if (pathIsSelect
                                                                            ) Color(
                                                                                0xFFEE8E00
                                                                            ) else Color.Transparent, // 蓝色背景
                                                                            shape = RoundedCornerShape(
                                                                                2.dp
                                                                            )
                                                                        ),
                                                                    contentAlignment = Alignment.Center

                                                                ) {
                                                                    if (pathIsSelect) {
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
                                    Text("确定要删除选中的 ${uiState.selectedPaths.size} 个文件吗？此操作不可恢复。")
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false

                                            scope.launch {

                                                if (multiSelectMode) {
                                                    // =========================
                                                    // 🟦 多选删除
                                                    // =========================

                                                    val pathsToDelete = uiState.selectedPaths

                                                    val filesToDelete = pathsToDelete.mapNotNull { sel ->
                                                        folderContent.files.find {
                                                            it.path == sel || it.file_url == sel
                                                        }
                                                    }

                                                    filesToDelete
                                                        .map { file ->
                                                            async {
                                                                RetrofitClient.getApi().deleteFile(file.path)
                                                            }
                                                        }
                                                        .awaitAll()

                                                    // ✅ 同步 UI
                                                    viewModel.deleteMultipleAndUpdateState(pathsToDelete)

                                                    multiSelectMode = false

//                                                    refreshFolder(uiState.currentPath)

                                                } else {
                                                    // =========================
                                                    // 🟨 单张删除（预览态）
                                                    // =========================

                                                    val pathToDelete = uiState.previewPath
                                                        ?: return@launch

                                                    val fileToDelete = folderContent.files.find {
                                                        it.net_url == pathToDelete
                                                    } ?: return@launch

                                                    val firstVisibleIndex = gridState.firstVisibleItemIndex
                                                    val firstVisibleOffset = gridState.firstVisibleItemScrollOffset

                                                    RetrofitClient.getApi().deleteFile(fileToDelete.path)

                                                    // ✅ 单删专用状态更新
                                                    viewModel.deleteSingleAndUpdatePreview(
                                                        file = fileToDelete
                                                    )

                                                    updateCacheSilently(uiState.currentPath)

                                                    gridState.scrollToItem(firstVisibleIndex, firstVisibleOffset)
                                                }
                                                Toast.makeText(context, "删除完成", Toast.LENGTH_SHORT).show()
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

                    }
                }
            }



            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White)
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
                                .fillMaxSize()
                                .combinedClickable(
                                    enabled = !multiSelectMode,

                                    onClick = {
                                        // 单击：切 Tab
                                        scope.launch {
                                            switchTab(
                                                newTab = displayName,
                                                defaultPath = defaultPath,
                                                uiState = uiState
                                            )
                                        }
                                    },

                                    onDoubleClick = {
                                        // 双击：回到顶部
                                        scope.launch {
                                            gridState.animateScrollToItem(0)
                                            refreshFolder(uiState.currentPath)
                                        }
                                    }
                                )

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

            if (showTextInputDialog) {
                Dialog(onDismissRequest = { showTextInputDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .widthIn(min = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Text(
                                text = "生图",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("请输入描述文本…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5,
                                maxLines = 7
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {

                                TextButton(
                                    onClick = {
                                        showTextInputDialog = false
                                    }
                                ) {
                                    Text("取消")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    enabled = inputText.isNotBlank(),
                                    onClick = {
                                        showTextInputDialog = false
                                        sendTextToGenerate(inputText)
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text("发送")
                                }

                            }
                        }
                    }
                }
            }

        }

        if (uiState.previewVideo != null) {
            isTopBarVisible = false
            val videoPath = uiState.previewVideo
            if (videoPath != null) {
                VideoDetailScreen(
                    videoPath,
                    onDismiss = {
                        viewModel.closePreviewVideo()
                        isTopBarVisible = true
                    }
                )
            }
        }

        if (showPerspective && perspectiveFiles != null) {
            PerspectiveScreen(
                files = perspectiveFiles!!,
                onClose = {
                    showPerspective = false
                    perspectiveFiles = null
                    showMoreSheet = false
                    isTopBarVisible = !isTopBarVisible
                }
            )
        }

        if (uiState.previewPath != null) {
            ImageDetailScreen(
                sortedFiles  = uiState.sortedFiles,
                initialIndex = uiState.currentIndex,
                onImageClick = {
                    isTopBarVisible = !isTopBarVisible
                    showMoreSheet = false
                },
                onSelectedFileChange = { path ->
                    val file = uiState.sortedFiles
                        .firstOrNull { it.net_url == path }

                    if (file != null && !file.is_dir) {
                        selectedFileForMenu = file

                        if (multiSelectMode) {
                            viewModel.toggleSelect(file)
                        } else {
                            val index = uiState.sortedFiles.indexOf(file)
                            if (index >= 0) {
                                viewModel.openPreview(file, index)
                            }

                            hideStates.clear()
                            hideStates[file.net_url ?: ""] = true
                        }
                    }
                },
                visibleCoordsMap = visibleCoordsMap,
                onRequestClose = {
                    overlayVisible = false
                    imageClosing = true
                },
                onCloseAnimationEnd = {
                    imageClosing = false
                    viewModel.closePreview()
                    viewModel.clearSelection()
                    hideStates.clear()
                    isTopBarVisible = true
                    overlayVisible = true
                },
            )

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
                thumbnailUrls = generateThumbnailUrls,
                navController = navController,
                onLoadingChange = { loading ->
                    isLoading = loading
                }
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
                                refreshFolder(uiState.currentPath)
                                Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
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
        currentIndex = uploadIndex,
        totalCount = totalCount,
    )


    if (showNudeSheet) {
        NudeModeBottomSheet(
            onDismiss = { showNudeSheet = false },
            onCreativeModeClick = { params ->
                scope.launch {
                    isLoading = true

                    try {
                        performNudeGeneration(
                            context = context,
                            selectedImages = uiState.selectedPaths.toList(),
                            folderContent = uiState.folderContent,
                            refreshFolder = {
                                scope.launch {
                                    refreshFolder(uiState.currentPath)
                                }
                            },
                            clearSelection = {
                                viewModel.clearSelection()
                                multiSelectMode = false
                            },
                            creativeMode = true,
                            params = params
                        )
                    } finally {
                        isLoading = false
                    }
                }
                showNudeSheet = false
            }
        )
    }

    if (isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "请稍后...",
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (isTopBarVisible && ((multiSelectMode && uiState.selectedPaths.isNotEmpty()) || uiState.previewPath?.isNotEmpty() == true)) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val itemWidth = screenWidth / 5
        fun hasMp4File(selectedPaths: Collection<String>): Boolean {
            return selectedPaths.any { path ->
                path.lowercase().endsWith(".mp4")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .graphicsLayer { alpha = 1 - bottomBarAlpha }
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(1f),
                color = Color.White,
                shadowElevation = 32.dp,     // 提升阴影
                tonalElevation = 8.dp       // 细腻分层
            ) {

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    item {
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.layers),
                            tint = Color.Black,
                            label = "差分",
                            contentDescription = "差分",
                            iconSize = 22.dp,
                            itemWidth = itemWidth,
                        ) {
                            if (hasMp4File(uiState.selectedPaths)) {
                                Toast.makeText(context, "视频无法进行此操作", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }
                            val selectedPaths = uiState.selectedPaths

                            if (selectedPaths.isEmpty()) {
                                Toast.makeText(context, "请选择图片", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            val selectedFiles = selectedPaths.mapNotNull { path ->
                                folderContent?.files?.find {
                                    it.file_url == path || it.path == path
                                }
                            }

                            if (selectedFiles.size != selectedPaths.size) {
                                Toast.makeText(context, "文件信息异常", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            val result = resolveDiffFilesWithCheck(
                                selectedFiles,
                                folderContent?.files ?: emptyList()
                            )

                            if (result == null) {
                                Toast.makeText(context, "图片匹配失败或分辨率异常", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            val (originFile, latestNudeFile) = result
//
//                            navController.currentBackStackEntry
//                                ?.savedStateHandle
//                                ?.set("perspective_files", listOf(originFile, latestNudeFile))
//
//                            navController.navigate("image_perspective")
//
//                            multiSelectMode = false
                            isTopBarVisible = !isTopBarVisible
                            showMoreSheet = false
                            perspectiveFiles = listOf(originFile, latestNudeFile)
                            showPerspective = true
                            multiSelectMode = false
                        }
                    }

                    // --- 换衣 ---
                    item {
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.clothes),
                            tint = Color.Black,
                            label = "脱衣",
                            contentDescription = "脱衣",
                            iconSize = 19.dp,
                            itemWidth = itemWidth,
                        ) {
                            if (hasMp4File(uiState.selectedPaths)) {
                                Toast.makeText(context, "视频无法进行此操作", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }

                            if (uiState.selectedPaths.isNotEmpty()) {
                                showNudeSheet = true
                            } else {
                                Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    item {
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.picture),
                            tint = Color.Black,
                            label = "修图",
                            contentDescription = "修图",
                            iconSize = 22.dp,
                            itemWidth = itemWidth,
                        ) {
                            if (hasMp4File(uiState.selectedPaths)) {
                                Toast.makeText(context, "视频无法进行此操作", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }
                            if (uiState.selectedPaths.isNotEmpty()) {
                                generateImageUrls = uiState.selectedPaths.mapNotNull { path ->
                                    folderContent?.files?.find { it.file_url == path || it.path == path }?.file_url
                                }
                                generateThumbnailUrls = uiState.selectedPaths.mapNotNull { path ->
                                    folderContent?.files?.find { it.file_url == path || it.path == path }?.thumbnail_url
                                }

                                showEditSheet = true  // ✅ 弹出修图界面
                            } else {
                                Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                            }
                            multiSelectMode = false
                        }
                    }

                    item {
                        IconActionButton(
                            iconPainter = painterResource(id = R.drawable.video),
                            tint = Color.Black,
                            label = "动图",
                            contentDescription = "动图",
                            iconSize = 20.dp,
                            itemWidth = itemWidth,
                        ) {
                            if (hasMp4File(uiState.selectedPaths)) {
                                Toast.makeText(context, "视频无法进行此操作", Toast.LENGTH_SHORT).show()
                                return@IconActionButton
                            }
                            if (uiState.selectedPaths.isNotEmpty()) {
                                generateImageUrls = uiState.selectedPaths.mapNotNull { path ->
                                    folderContent?.files?.find { it.file_url == path || it.path == path }?.file_url
                                }
                                generateThumbnailUrls = uiState.selectedPaths.mapNotNull { path ->
                                    folderContent?.files?.find { it.file_url == path || it.path == path }?.thumbnail_url
                                }

                                showGenerateSheet = true
                            } else {
                                Toast.makeText(context, "未选中任何图片", Toast.LENGTH_SHORT).show()
                            }
                            multiSelectMode = false
                        }
                    }

                    item {
                        IconActionButton(
                            iconVector = Icons.Default.MoreVert, // 你的更多图标
                            tint = Color.Black,
                            label = "更多",
                            contentDescription = "更多",
                            iconSize = 19.dp,
                            itemWidth = itemWidth,
                        ) {
                            showMoreSheet = !showMoreSheet
                        }
                    }

                }
            }

            var downloadDialogVisible by remember { mutableStateOf(false) }
            var currentDownloadingFile by remember { mutableStateOf("") }
            var currentIndex by remember { mutableIntStateOf(0) }
            val totalCount = uiState.selectedPaths.size

            // 浮层放在独立 BoxScope 内，不撑满父布局
            if (showMoreSheet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 72.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .width(180.dp)
                            .wrapContentHeight(), // 自适应高度
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 把按钮列表分成每行最多 3 个
                            val itemsList = mutableListOf<@Composable () -> Unit>()


                            itemsList.add {
                                IconActionButton(
                                    iconPainter = painterResource(id = R.drawable.copy_paste),
                                    tint = Color.Black,
                                    label = "复制",
                                    contentDescription = "复制",
                                    iconSize = 23.dp,
                                    itemWidth = itemWidth,
                                ) {
                                    showCutDialog = true
                                    copyOrCut = "copy"
                                }
                            }

                            itemsList.add {
                                IconActionButton(
                                    iconPainter = painterResource(id = R.drawable.cut),
                                    tint = Color.Black,
                                    label = "剪切",
                                    contentDescription = "剪切",
                                    iconSize = 24.dp,
                                    itemWidth = itemWidth,
                                ) {
                                    showCutDialog = true
                                    copyOrCut = "cut"
                                }
                            }

                            itemsList.add {
                                IconActionButton(
                                    iconPainter = painterResource(id = R.drawable.download),
                                    tint = Color.Black,
                                    label = "下载",
                                    contentDescription = "下载",
                                    iconSize = 20.dp,
                                    itemWidth = itemWidth,
                                ) {
                                    if (uiState.selectedPaths.isNotEmpty()) {
                                        scope.launch {
                                            downloadDialogVisible = true
                                            currentIndex = 0
                                            uiState.selectedPaths.forEachIndexed { index, imagePath ->
                                                currentIndex = index + 1
                                                currentDownloadingFile = imagePath.substringAfterLast("/")
                                                try {
                                                    val fullUrl = "${ServerConfig.baseUrl}$imagePath"
                                                    val filename = imagePath.substringAfterLast("/")

                                                    withContext(Dispatchers.IO) {
                                                        val request = okhttp3.Request.Builder().url(fullUrl).build()
                                                        val response = okhttp3.OkHttpClient().newCall(request).execute()
                                                        if (!response.isSuccessful) throw Exception("下载失败")

                                                        response.body.byteStream().use { inputStream ->
                                                            val savedUri = saveFileToGallery(context, inputStream, filename)
                                                            withContext(Dispatchers.Main) {
                                                                if (savedUri == null) {
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
                                    showMoreSheet = false
                                }
                            }

                            itemsList.add {

                                IconActionButton(
                                    iconPainter = painterResource(id = R.drawable.share),
                                    tint = Color.Black,
                                    label = "分享",
                                    contentDescription = "分享",
                                    iconSize = 20.dp,
                                    itemWidth = itemWidth,
                                ) {
                                    if (uiState.selectedPaths.size != 1) {
                                        Toast.makeText(context, "请只选择一张图片", Toast.LENGTH_SHORT).show()
                                        return@IconActionButton
                                    }

                                    val serverPath = "${ServerConfig.baseUrl}${uiState.selectedPaths.first()}"
                                    val fileName = File(serverPath).name
                                    val localFile = File(context.cacheDir, fileName)

                                    // 使用协程下载文件
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                if (!localFile.exists()) {
                                                    URL(serverPath).openStream().use { input ->
                                                        localFile.outputStream().use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                }
                                            }

                                            withContext(Dispatchers.Main) {
                                                // 获取 FileProvider URI
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "com.kano.mycomfyui.fileprovider",
                                                    localFile
                                                )

                                                // 创建系统分享 Intent
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/*"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                // 弹出分享面板
                                                context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }

                            itemsList.add {
                                IconActionButton(
                                    iconPainter = painterResource(id = R.drawable.delete),
                                    tint = Color.Black,
                                    label = "删除",
                                    contentDescription = "删除",
                                    iconSize = 24.dp,
                                    itemWidth = itemWidth,
                                ) {
                                    if (uiState.selectedPaths.isNotEmpty()) {
                                        showDeleteDialog = true
                                    } else {
                                        Toast.makeText(context, "没有可删除的文件", Toast.LENGTH_SHORT).show()
                                    }
                                    showMoreSheet = false
                                }
                            }

                            // --- 按钮列表拆成每行 3 个 ---
                            itemsList.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            item()
                                        }
                                    }
                                    // 如果一行不足 3 个，用 Spacer 补齐
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            ProgressDialog(
                visible = downloadDialogVisible,
                title = "正在下载",
                fileName = currentDownloadingFile,
                currentIndex = currentIndex,
                totalCount = totalCount,
            )
        }
    }



    if (showCutDialog) {
        FolderPickerDialog(
            initialPath = ".",
            onCancel = { showCutDialog = false },
            onConfirm = { targetDir ->
                showCutDialog = false

                val selectedFiles = uiState.selectedPaths.mapNotNull { path ->
                    folderContent?.files?.find { it.file_url == path || it.path == path }?.path
                }

                if (selectedFiles.isEmpty()) {
                    Toast.makeText(context, "未选中文件", Toast.LENGTH_SHORT).show()
                    return@FolderPickerDialog
                }

                if (targetDir == uiState.currentPath) {
                    Toast.makeText(context, "目标文件夹与当前位置相同", Toast.LENGTH_SHORT).show()
                    return@FolderPickerDialog
                }

                scope.launch {
                    viewModel.clearSelection()
                    multiSelectMode = false
                    showMoreSheet = false
                    if (copyOrCut == "copy"){
                        selectedFiles.forEach { filePath ->
                            try {
                                RetrofitClient.getApi().copyFile(src = filePath, dest = targetDir)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "复制失败: $filePath", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Toast.makeText(context, "已复制 ${selectedFiles.size} 项", Toast.LENGTH_SHORT).show()
                    } else if (copyOrCut == "cut"){
                        selectedFiles.forEach { filePath ->
                            try {
                                RetrofitClient.getApi().moveFile(src = filePath, dest = targetDir)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "移动失败: $filePath", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Toast.makeText(context, "已移动 ${selectedFiles.size} 项", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.clearSelection()
                    refreshFolder(uiState.currentPath)
                    if (currentTab == "素材"){
                        refreshFolder(targetDir)
                    }
                }
            }
        )
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
                                        parent = uiState.currentPath,
                                        name = folderName
                                    )

                                    Toast.makeText(context, "文件夹已创建", Toast.LENGTH_SHORT).show()

                                    // 刷新文件夹
                                    scope.launch {
                                        refreshFolder(uiState.currentPath)
                                    }

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
    iconSize: Dp = 18.dp,
    iconBoxHeight: Dp = 24.dp,
    itemWidth: Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .width(itemWidth)
            .wrapContentHeight()
    ) {
        // ✅ 固定图标区域，不随图标大小改变
        Box(
            modifier = Modifier
                .height(iconBoxHeight), // 固定高度
            contentAlignment = Alignment.Center // 垂直 + 水平居中
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


@SuppressLint("FrequentlyChangingValue")
@Composable
fun GridWithVerticalScrollHandleOverlay(
    modifier: Modifier,
    allItems: List<FileInfo>,
    columns: Int = 3,
    gridState: LazyGridState,
    handleHeight: Dp = 40.dp, // 滑块高度
    handleWidth: Dp = 28.dp, // 滑块宽度
    trackPaddingTop: Dp = 110.dp, // 轨道顶部 padding
    trackPaddingBottom: Dp = 68.dp, // 轨道底部 padding
    gridPaddingTop: Dp = 100.dp, // 轨道顶部 padding
    gridPaddingBottom: Dp = 64.dp, // 轨道底部 padding
    content: @Composable (LazyGridState) -> Unit
) {

    val scope = rememberCoroutineScope()
    var handleOffset by remember { mutableStateOf(0f) }
    var trackHeightPx by remember { mutableStateOf(0f) }
    var gridWidthPx by remember { mutableStateOf(0f) }
    val handleHeightPx = with(LocalDensity.current) { handleHeight.toPx() }
    val paddingTopPx = with(LocalDensity.current) { trackPaddingTop.toPx() }
    val paddingBottomPx = with(LocalDensity.current) { trackPaddingBottom.toPx() }
    var isDragging by remember { mutableStateOf(false) }

    val showHandle by remember(allItems.size, gridWidthPx, trackHeightPx) {
        derivedStateOf {
            val rowHeightPx = if (columns > 0) gridWidthPx / columns else 0f
            val totalRows = ceil(allItems.size / columns.toFloat())
            val totalHeightPx = totalRows * rowHeightPx
            totalHeightPx > 2 * trackHeightPx
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ------------------ Grid 内容 ------------------
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = gridPaddingTop, bottom = gridPaddingBottom)
                .onGloballyPositioned { coords ->
                    gridWidthPx = coords.size.width.toFloat()
                },

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
                    trackHeightPx =
                        coords.size.height.toFloat() - paddingTopPx - paddingBottomPx
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

                    val scrollY =
                        (handleOffset / (trackHeightPx - handleHeightPx)) * (totalHeightPx - trackHeightPx)

                    val targetRowF = scrollY / rowHeightPx
                    val targetRow = targetRowF.toInt()
                        .coerceIn(0, totalRows.toInt() - 1)

                    val rowOffset = ((targetRowF - targetRow) * rowHeightPx).toInt()
                    val targetIndex = targetRow * columns

                    scope.launch {
                        gridState.scrollToItem(targetIndex, rowOffset)
                    }
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(0, (handleOffset + paddingTopPx).roundToInt())
                        }
                        .width(handleWidth)
                        .height(handleHeight)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp
                            ),
                            clip = false
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp
                            )
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = dragState,
                            onDragStopped = {
                                isDragging = false
                            }
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
//                    showHandle = totalHeightPx > 3 * trackHeightPx

                    if (showHandle) {
                        if (!isDragging) {
                            val scrollY =
                                (gridState.firstVisibleItemIndex / columns) * rowHeightPx +
                                        gridState.firstVisibleItemScrollOffset.toFloat()

                            handleOffset =
                                (scrollY / (totalHeightPx - trackHeightPx)) *
                                        (trackHeightPx - handleHeightPx)
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


