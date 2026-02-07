package com.kano.mycomfyui.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
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
    val result: String?
)

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

    // BottomSheet 是否展开
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    suspend fun loadTasks() {
        try {
            val api = RetrofitClient.getApi()
            val resp = api.getTasks()

            val formatter = DateTimeFormatter.ISO_DATE_TIME
            taskList = resp.sortedByDescending { task ->
                try {
                    task.start_time?.let { LocalDateTime.parse(it, formatter) } ?: LocalDateTime.MIN
                } catch (e: Exception) {
                    LocalDateTime.MIN
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restartTask(id: String) {
        try {
            val api = RetrofitClient.getApi()
            val resp = api.restartTask(id)
            if (resp.success) loadTasks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(navBackStackEntry) {
        while (isActive) { // 使用 isActive 判断协程是否仍然活跃
            try {
                loadTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(1000) // 每 1 秒刷新一次
        }
    }


    setTopBar("任务管理", false, false, {}, {}, {}, {})

    Scaffold(
        topBar = {
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
                                val response = api.clearTasks()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "清空任务",
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp), // 自动适配列数
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(taskList) { task ->
                TaskGridItem(
                    task = task,
                    onClick = {
                        selectedTask = task
                        scope.launch {
                            sheetState.show()
                        }
                    }
                )
            }
        }

        selectedTask?.let { task ->
            ModalBottomSheet(
                onDismissRequest = {
                    selectedTask = null
                },
                sheetState = sheetState
            ) {
                TaskDetailContent(
                    task = task,
                    onRestart = {
                        scope.launch {
                            restartTask(task.id)
                        }
                    }
                )
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
            "正在执行" -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
            }
            "执行成功" -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
            "执行失败" -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            "追加任务" -> {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = Color(0xFF4C79AF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun TaskDetailContent(
    task: TaskInfo,
    onRestart: () -> Unit
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
            if (!task.result.isNullOrEmpty()) {
                PreviewImage(
                    url = "${ServerConfig.baseUrl}${task.result}",
                    onClick = { previewUrl = it }
                )
            }
        }



        TaskInfoSection(task)
    }

    // ===== 全屏预览 Dialog =====
    previewUrl?.let { url ->
        FullScreenImageDialog(
            imageUrl = url,
            onDismiss = { previewUrl = null }
        )
    }
}



fun formatTime(raw: String): String {
    return raw.replace("T", " ").substringBefore(".")
}


@Composable
fun TaskInfoSection(task: TaskInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // ===== 状态行 =====
        Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (task.status) {
                "执行成功" -> Color(0xFF4CAF50)
                "执行失败", "执行超时" -> Color(0xFFF44336)
                "正在执行" -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )

            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = task.status,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                fontSize = 20.sp
            )
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
                label = "开始时间",
                value = formatTime(it)
            )
        }

        task.end_time?.let {
            InfoRow(
                label = "结束时间",
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
