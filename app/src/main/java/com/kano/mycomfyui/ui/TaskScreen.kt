package com.kano.mycomfyui.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kano.mycomfyui.R
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TaskInfo(
    val id: String,
    val func: String,
    val status: String,
    val retries: Int,
    val error: String?,
    val imageUrl: String?,
    val text: String?,
    val task_type: String?,
    val thumbnailUrl: String?,
    val start_time: String?,
    val end_time: String?,
    val run_time: String?,
    val result: String?
)

enum class TaskStatus(val label: String, val value: String) {
    RUNNING("正在执行", "RUNNING"),
    SUCCESS("执行成功", "SUCCESS"),
    PENDING("等待执行", "PENDING"),
    FAILED("执行失败", "FAILED");

    companion object {
        fun fromValue(value: String?): TaskStatus? {
            return entries.find { it.value == value }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    navController: NavHostController,
    setTopBar: (
        title: String,
        showRefresh: Boolean,
        showSetting: Boolean,
        refreshClick: () -> Unit,
        onAddImageClick: () -> Unit,
        onTaskManageClick: () -> Unit,
        onLockClick: () -> Unit
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var taskList by remember { mutableStateOf(listOf<TaskInfo>()) }
    val navBackStackEntry = remember { navController.currentBackStackEntry }
    // 当前选中的任务，用于 BottomSheet
    var selectedTask by remember { mutableStateOf<TaskInfo?>(null) }
    var currentStatus by remember { mutableStateOf(TaskStatus.RUNNING) }

    // BottomSheet 是否展开
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // lazy
    var page by remember { mutableStateOf(1) }          // 当前页码
    val pageSize = 20                                   // 每页数量
    var isLoading by remember { mutableStateOf(false) } // 是否正在加载
    var endReached by remember { mutableStateOf(false) } // 是否已经没有更多数据
    var isRefreshing by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 5 } // 超过5条才显示
    }

    suspend fun loadTasks(reset: Boolean = false) {
        if (isLoading) return
        isLoading = true

        if (reset) {
            page = 1
            endReached = false
        }

        try {
            val api = RetrofitClient.getApi()
            val resp = api.getTasks(
                page = page,
                size = pageSize,
                status = currentStatus.value   // 👈 关键
            )

            if (reset) {
                taskList = resp
            } else {
                taskList = (taskList + resp).distinctBy { it.id }
            }

            page++

            if (resp.size < pageSize) {
                endReached = true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 👇 防止崩溃
            endReached = true
        } finally {
            isLoading = false
        }
    }



    // ---- 顶层统一操作函数 ----
    fun handleDeleteTask(taskId: String) {
        scope.launch {
            try {
                val api = RetrofitClient.getApi()
                api.deleteTask(taskId)
                loadTasks(true)
                selectedTask = null
                sheetState.hide()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun handlePinTask(taskId: String) {
        scope.launch {
            try {
                val api = RetrofitClient.getApi()
                val resp = api.pinTask(taskId)
                if (resp.status == "success") {
                    // 刷新任务列表
                    loadTasks(true)
                    selectedTask = null
                    sheetState.hide()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun handleRestartTask(taskId: String) {
        scope.launch {
            try {
                val api = RetrofitClient.getApi()
                val resp = api.restartTask(taskId)
                if (resp.success) {
                    loadTasks(true)
                    selectedTask = null
                    sheetState.hide()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }


    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastVisibleItem =
                gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalCount = gridState.layoutInfo.totalItemsCount

            lastVisibleItem to totalCount
        }
            .collect { (lastVisibleItem, totalCount) ->

                if (lastVisibleItem != null &&
                    lastVisibleItem >= totalCount - 3 &&
                    !isLoading &&
                    !endReached
                ) {
                    loadTasks()
                }
            }
    }


    LaunchedEffect(Unit) {
        loadTasks(reset = true)
    }


    setTopBar("任务管理", false, false, {}, {}, {}, {})

    // ---- 下拉刷新状态 ----
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("任务管理") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black
                    ),
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val api = RetrofitClient.getApi()
                                    api.clearTasks()
                                    loadTasks(reset = true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "清空任务",
                                tint = Color.Black,
                            )
                        }

                        IconButton(onClick = {
                            scope.launch {
                                try {
                                    val api = RetrofitClient.getApi()
                                    api.deleteTasks()
                                    loadTasks(reset = true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除任务",
                                tint = Color.Black,
                                modifier = Modifier.height(22.dp)
                            )
                        }
                    }
                )

                // 👇 必须在 Column 里
                StatusTabBar(
                    current = currentStatus,
                    onSelect = { status ->
                        if (currentStatus != status) {
                            currentStatus = status
                            scope.launch {
                                loadTasks(reset = true)
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        loadTasks(reset = true)
                        delay(300)
                        isRefreshing = false
                    }
                },
                state = pullRefreshState,
                indicator = {
                    Indicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 0.dp),
                        isRefreshing = isRefreshing,
                        containerColor = Color.White,
                        color = Color(0xFF0066FF),
                        state = pullRefreshState
                    )
                },
                modifier = Modifier.fillMaxSize().padding(top = 150.dp)
            ) {

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    contentPadding = PaddingValues(
                        bottom = 16.dp // 底部空白
                    )
                ) {
                    items(taskList) { task ->
                        TaskGridItem(
                            task = task,
                            onClick = {
                                selectedTask = task
                                scope.launch { sheetState.show() }
                            }
                        )
                    }

//                    if (isLoading) {
//                        item {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(16.dp),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                CircularProgressIndicator()
//                            }
//                        }
//                    }


                }
            }
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { gridState.animateScrollToItem(0) }
                    },
                    modifier = Modifier
                        .padding(bottom = 48.dp, end = 24.dp)
                        .align(Alignment.BottomEnd)
                        .size(52.dp),
                    containerColor = Color.White,  // 半透明白色背景
                    contentColor = Color.Black, // 主题主色图标
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp) // 提升浮动感
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.top),
                        contentDescription = "回到顶部",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }



        selectedTask?.let { task ->
            ModalBottomSheet(
                onDismissRequest = {
                    selectedTask = null
                },
                sheetState = sheetState
            ) {
                var previewUrl by remember { mutableStateOf<String?>(null) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ===== 图片行 =====
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 16.dp,
                            alignment = Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 原图
                        if (!task.imageUrl.isNullOrEmpty()) {
                            PreviewImage(
                                url = "${ServerConfig.baseUrl}${task.thumbnailUrl}",
                                onClick = { previewUrl = it }
                            )
                        }

                        // 结果图
                        if (!task.result.isNullOrEmpty() && !task.result.endsWith(".mp4", ignoreCase = true)) {
                            PreviewImage(
                                url = "${ServerConfig.baseUrl}${task.result}",
                                onClick = { previewUrl = it }
                            )
                        }
                    }



                    TaskInfoSection(
                        task,
                        onRestart = { handleRestartTask(it.id) },
                        onDelete = { handleDeleteTask(it.id) },
                        onPinTop = { handlePinTask(it.id) }
                    )
                }

                // ===== 全屏预览 Dialog =====
                previewUrl?.let { url ->
                    FullScreenImageDialog(
                        imageUrl = url,
                        onDismiss = { previewUrl = null }
                    )
                }
            }
        }
    }
}


@Composable
fun TaskGridItem(
    task: TaskInfo,
    onClick: () -> Unit  // 新增点击回调

) {
    val imageUrl = remember(task) {
        if (task.thumbnailUrl != "null") {
            "${ServerConfig.baseUrl}${task.thumbnailUrl}"
        } else {
            "${ServerConfig.baseUrl}${task.imageUrl}"
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f) // 正方形
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1️⃣ 图片
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2️⃣ 右上角状态图标（带半透明圆形背景）
            StatusOverlay(
                status = task.status,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )

            // 3️⃣ 右下角文字
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = task.task_type ?: "",
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
fun StatusOverlay(
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = Color.White.copy(alpha = 1f), // 半透明黑色背景
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            "RUNNING" -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
            }

            "SUCCESS" -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            "FAILED" -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }

            "PENDING" -> {
                Icon(
                    painter = painterResource(id = R.drawable.clock),
                    contentDescription = null,
                    tint = Color(0xFFFFAB72),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}



fun formatTime(raw: String): String {
    return raw.replace("T", " ").substringBefore(".")
}


@Composable
fun TaskInfoSection(
    task: TaskInfo,
    onRestart: (TaskInfo) -> Unit = {},
    onDelete: (TaskInfo) -> Unit = {},
    onPinTop: (TaskInfo) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // ===== 状态行 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (task.status) {
                "SUCCESS" -> Color(0xFF4CAF50)
                "FAILED" -> Color(0xFFF44336)
                "RUNNING" -> Color(0xFF0099FF)
                "PENDING" -> Color(0xFFFF9C57)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }


            // 状态圆点
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 状态文字
            val statusEnum = TaskStatus.fromValue(task.status)

            Text(
                text = statusEnum?.label ?: task.status,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                fontSize = 20.sp
            )


            Spacer(modifier = Modifier.weight(1f)) // 推按钮到右边

            // ===== 按钮区域 =====
            IconButton(onClick = { onRestart(task) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新"
                )
            }

            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除"
                )
            }

            IconButton(onClick = { onPinTop(task) }) {
                Icon(
                    painter = painterResource(id = R.drawable.top),
                    contentDescription = "置顶",
                    modifier = Modifier.height(22.dp)
                )
            }
        }

        Divider()

        // ===== 任务类型 =====
        InfoRow(
            label = "任务类型",
            value = task.task_type ?: "-"
        )

        // ===== 任务参数 =====
        if (!task.text.isNullOrBlank()) {
            InfoRow(
                label = "任务参数",
                value = task.text,
                multiline = true
            )
        }

        // ===== 错误信息 =====
        if (!task.error.isNullOrBlank()) {
            InfoRow(
                label = "错误信息",
                value = task.error,
                multiline = true,
                valueColor = MaterialTheme.colorScheme.error
            )
        }

        // ===== 时间 =====
        task.start_time?.let {
            InfoRow(
                label = "任务加入时间",
                value = formatTime(it)
            )
        }

        task.run_time?.let {
            InfoRow(
                label = "任务开始时间",
                value = formatTime(it)
            )
        }

        task.end_time?.let {
            InfoRow(
                label = "任务结束时间",
                value = formatTime(it)
            )
        }
    }
}


@Composable
fun InfoRow(
    label: String,
    value: String,
    multiline: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = if (multiline) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PreviewImage(
    url: String,
    onClick: (String) -> Unit
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .size(150.dp)
            .aspectRatio(1f)
            .clip(RectangleShape)
            .clickable { onClick(url) },
        contentScale = ContentScale.Crop
    )
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.5f))
        ) {

            val scale = remember { mutableStateOf(1f) }
            val offset = remember { mutableStateOf(Offset.Zero) }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        translationX = offset.value.x
                        translationY = offset.value.y
                    }
                    .pointerInput(Unit) {
                        onDismiss()
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun StatusTabBar(
    current: TaskStatus,
    onSelect: (TaskStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TaskStatus.entries.forEach { status ->

            val selected = status == current

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(status) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {

                Text(
                    text = status.label,
                    color = if (selected) Color(0xFF0066FF) else Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(48.dp)   // 🔥 固定宽度，不要 fillMaxWidth
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (selected) Color(0xFF0066FF)
                            else Color.Transparent
                        )
                )
            }
        }

    }
}
