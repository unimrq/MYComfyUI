package com.kano.mycomfyui.ui

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
    var maskClothesEnabled by remember { mutableStateOf(loadMaskClothesEnabled(context)) }
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

            SettingSwitchCard(
                title = "图生视频功能",
                checked = videoGenEnabled,
                onCheckedChange = {
                    videoGenEnabled = it
                    saveVideoGenEnabled(context, it)
                }
            )

            SettingSwitchCard(
                title = "蒙版换衣功能",
                checked = maskClothesEnabled,
                onCheckedChange = {
                    maskClothesEnabled = it
                    saveMaskClothesEnabled(context, it)
                }
            )

            SettingSwitchCard(
                title = "文生图功能",
                checked = text2ImgEnabled,
                onCheckedChange = {
                    text2ImgEnabled = it
                    saveText2ImgEnabled(context, it)
                }
            )
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
