package com.kano.mycomfyui.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kano.mycomfyui.network.ApiService
import com.kano.mycomfyui.network.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ServerStatusCard()

            SettingsCard("地址设置", icon = Icons.Default.Home) {
                navController.navigate("address_settings")
            }
            SettingsCard("功能设置", icon = Icons.Default.Settings) {
                navController.navigate("function_settings")
            }
            SettingsCard("提示词", icon = Icons.Default.Build) {
                navController.navigate("prompt_list")
            }
            SettingsCard("帮助", icon = Icons.Default.Info) {
                navController.navigate("help")
            }

        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector? = null, // 可选图标
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
        ) {
            // 左侧图标
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF1976D2), // 可以改颜色
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 标题
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            // 右侧箭头
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "前往",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

enum class ServerStatus {
    LOADING,
    ONLINE,
    OFFLINE
}


@Composable
fun ServerStatusCard() {
    var status by remember { mutableStateOf(ServerStatus.LOADING) }
    val api = RetrofitClient.getApi()

    LaunchedEffect(Unit) {
        status = try {
            val alive = try {
                api.getServerStatus().alive
            } catch (e: Exception) {
                false
            }
            Log.d("getServerStatus", alive.toString())
            if (alive) ServerStatus.ONLINE else ServerStatus.OFFLINE
        } catch (e: Exception) {
            ServerStatus.OFFLINE
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ServerStatus.ONLINE -> Color(0xFFE8F5E9)
                ServerStatus.OFFLINE -> Color(0xFFFFEBEE)
                ServerStatus.LOADING -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = when (status) {
                    ServerStatus.ONLINE -> Icons.Default.CheckCircle
                    ServerStatus.OFFLINE -> Icons.Default.Clear
                    ServerStatus.LOADING -> Icons.Default.Refresh
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when (status) {
                    ServerStatus.ONLINE -> Color(0xFF4CAF50)
                    ServerStatus.OFFLINE -> Color(0xFFF44336)
                    ServerStatus.LOADING -> Color.Gray
                }
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = when (status) {
                    ServerStatus.ONLINE -> "服务器运行中"
                    ServerStatus.OFFLINE -> "服务器未运行"
                    ServerStatus.LOADING -> "正在检测服务器状态..."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

