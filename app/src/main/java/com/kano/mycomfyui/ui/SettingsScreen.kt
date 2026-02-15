package com.kano.mycomfyui.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kano.mycomfyui.R
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
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ServerStatusCard()

            SettingsCard(
                title = "地址设置",
                iconVector = Icons.Default.Home
            ) {
                navController.navigate("address_settings")
            }

            SettingsCard(
                title = "提示词",
                iconPainter = painterResource(R.drawable.word)
            ) {
                navController.navigate("prompt_list")
            }

            SettingsCard(
                title = "Qwen设置",
                iconVector = Icons.Default.Build
            ) {
                navController.navigate("qwen")
            }


        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {

            when {
                iconVector != null -> {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF3965B0) // 你想要的颜色
                    )

                }

                iconPainter != null -> {
                    Image(
                        painter = iconPainter,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(
                            Color(0xFF3965B0),
                            blendMode = BlendMode.SrcIn
                        )
                    )

                }
            }

            if (iconVector != null || iconPainter != null) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f)
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
                Log.e("debug", e.message.toString())
                false
            }
            Log.d("getServerStatus", alive.toString())
            if (alive) ServerStatus.ONLINE else ServerStatus.OFFLINE
        } catch (e: Exception) {
            Log.e("debug", e.message.toString())
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

