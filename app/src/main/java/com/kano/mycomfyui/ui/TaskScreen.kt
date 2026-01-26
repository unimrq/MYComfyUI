package com.kano.mycomfyui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            items(taskList) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                    ,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White // 设置 Card 背景为白色
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (task.task_type != "文生图"){
                            // 左侧图片，裁剪为正方形
                            AsyncImage(
                                model = "${ServerConfig.baseUrl}${task.thumbnailUrl}",
                                contentDescription = "任务图片",
                                modifier = Modifier
                                    .size(108.dp)
                                    .padding(end = 0.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop // 裁剪
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                        }

                        // 右侧文字左对齐
                        Column(
                            modifier = Modifier
                                .weight(1f) // 占据剩余空间
                                .padding(start = 4.dp), // 左侧额外 padding
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            val statusColor = when (task.status) {
                                "正在执行" -> Color.Red
                                "执行成功" -> Color(0xFF2E7D32)
                                "执行失败" -> Color.Black
                                "执行超时" -> Color.Black
                                else -> Color.Blue
                            }

                            Text(
                                text = task.status,
                                style = MaterialTheme.typography.bodyLarge,
                                color = statusColor
                            )

                            Spacer(modifier = Modifier.padding(4.dp))

                            Text(
                                text = buildString {
                                    append("类别：${task.task_type ?: ""}")

                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!task.text.isNullOrEmpty()) {
                                val displayText = if (task.text.length > 23) task.text.take(23) + "…" else task.text
                                Text(
                                    text = "参数：$displayText",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            task.start_time?.let { datetime ->
                                // 把 "2025-11-10T22:29:04.999765" 转成 "2025-11-10 22:29:04"
                                val formatted = datetime.replace("T", " ").split(".")[0]

                                Text(
                                    text = "时间：$formatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }


                        }

                        // 右侧重放按钮
                        IconButton(onClick = { scope.launch { restartTask(task.id) } }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重放"
                            )
                        }
                    }
                }
            }

        }
    }
}
