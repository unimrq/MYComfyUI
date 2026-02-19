package com.kano.mycomfyui.ui


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kano.mycomfyui.R
import com.kano.mycomfyui.network.LogWebSocket
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerInfoScreen() {
    val context = LocalContext.current
    val api = RetrofitClient.getApi()
    val scope = rememberCoroutineScope()

    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }


    val list = loadAddressList(context)       // 获取所有地址
    val selectedIndex = getSavedAddress(context)?.toIntOrNull() ?: 0
    val currentAddress = list.getOrNull(selectedIndex) ?: list.first()


    // ---------- WebSocket 方式接收日志 ----------
    val ws = remember(currentAddress.address) {
        LogWebSocket("ws://${currentAddress.address}/ws/logs") { message ->
            scope.launch {
                logs = (listOf(message) + logs).take(200)
            }
        }
    }


    LaunchedEffect(currentAddress.address) {

        // 1️⃣ 先拉历史日志
        try {
            val history = api.getLogs(200)
            logs = history.reversed()   // ⚠ 关键：因为你要新日志在最上
        } catch (e: Exception) {
            Log.e("Log", "获取历史失败", e)
        }

        // 2️⃣ 再连接 WebSocket
        ws.connect()
    }


    DisposableEffect(currentAddress.address) {
        onDispose { ws.close() }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器信息") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                ActionButton(
                    text = "扫描",
                    isRunning = isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        isRunning = true
                        try {
                            api.scanFolder()
                            Toast.makeText(context, "扫描已开始", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "扫描失败：${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isRunning = false
                        }
                    }
                }

                ActionButton(
                    text = "检测",
                    isRunning = isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        isRunning = true
                        try {
                            api.detectFolder()
                            Toast.makeText(context, "检测已开始", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "检测失败：${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isRunning = false
                        }
                    }
                }

                ActionButton(
                    text = "缩略图",
                    isRunning = isRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        isRunning = true
                        try {
                            api.generateThumbnails()
                            Toast.makeText(context, "缩略图任务已开始", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "缩略图生成失败：${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isRunning = false
                        }
                    }
                }
            }


            // ---------- 日志显示区域 ----------
            Text("操作日志", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f), // 填满剩余空间
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {
                    logs.forEach { line ->
                        Text(line, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = !isRunning) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color.LightGray else MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

