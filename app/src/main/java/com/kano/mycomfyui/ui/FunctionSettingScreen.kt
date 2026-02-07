package com.kano.mycomfyui.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var text2ImgEnabled by remember { mutableStateOf(loadText2ImgEnabled(context)) }

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

            var showKeyDialog by remember { mutableStateOf(false) }
            var pendingEnableAction by remember { mutableStateOf<(() -> Unit)?>(null) }


            SettingSwitchCard(
                title = "图生视频功能",
                checked = videoGenEnabled,
                onCheckedChange = { target ->
                    if (target) {
                        // 尝试开启 → 需要密钥
                        pendingEnableAction = {
                            videoGenEnabled = true
                            saveVideoGenEnabled(context, true)
                        }
                        showKeyDialog = true
                    } else {
                        // 关闭不需要密钥
                        videoGenEnabled = false
                        saveVideoGenEnabled(context, false)
                    }
                },
                description = "Wan2.2 功能开发中"
            )


            SettingSwitchCard(
                title = "文生图功能",
                checked = text2ImgEnabled,
                onCheckedChange = { target ->
                    if (target) {
                        // 尝试开启 → 需要密钥
                        pendingEnableAction = {
                            text2ImgEnabled = true
                            saveText2ImgEnabled(context, true)
                        }
                        showKeyDialog = true
                    } else {
                        // 关闭不需要密钥
                        text2ImgEnabled = false
                        saveText2ImgEnabled(context, false)
                    }
                },
                description = "Z-Image-Turbo 功能开发中"
            )


            if (showKeyDialog) {
                SecretKeyDialog(
                    onConfirm = { key ->
                        if (key == "root@1234") {
                            pendingEnableAction?.invoke()
                        }
                        showKeyDialog = false
                        pendingEnableAction = null
                    },
                    onDismiss = {
                        showKeyDialog = false
                        pendingEnableAction = null
                    }
                )
            }

        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    checked: Boolean,
    description: String = "",
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
                .padding(14.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF3965B0),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.8f) // 缩小到原来的 80%
                )

            }
            if (!description.isEmpty()){
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }

        }
    }
}


@Composable
fun SecretKeyDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("请输入密钥") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("密钥") },
                singleLine = true
            )
        },
        confirmButton = {
            Text(
                text = "确认",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onConfirm(input) }
            )
        },
        dismissButton = {
            Text(
                text = "取消",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onDismiss() }
            )
        }
    )
}
