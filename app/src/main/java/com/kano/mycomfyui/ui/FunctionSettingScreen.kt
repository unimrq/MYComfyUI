package com.kano.mycomfyui.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionSettingScreen() {

    val context = LocalContext.current

    // 读取 SharedPreferences
    var videoGenEnabled by remember { mutableStateOf(loadVideoGenEnabled(context)) }
    var maskClothesEnabled by remember { mutableStateOf(loadMaskClothesEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("功能设置", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SettingSwitchCard(
                title = "图生视频功能",
                checked = videoGenEnabled,
                description = "开启后在长按图片的弹出菜单中新增一项“动图”，此功能要求服务器开放视频生成权限",
                onCheckedChange = {
                    videoGenEnabled = it
                    saveVideoGenEnabled(context, it)
                }
            )

            SettingSwitchCard(
                title = "蒙版换衣功能",
                checked = maskClothesEnabled,
                description = "开启后在换衣功能中开放“蒙版模式”，此功能要求服务器开放蒙版换衣生成权限",
                onCheckedChange = {
                    maskClothesEnabled = it
                    saveMaskClothesEnabled(context, it)
                }
            )
        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    checked: Boolean,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.8f) // 缩小到原来的 80%
                )

            }

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}
