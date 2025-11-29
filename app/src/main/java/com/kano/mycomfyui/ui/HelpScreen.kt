package com.kano.mycomfyui.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {

    // 章节数据
    val sections = listOf(
        HelpSection(
            title = "APP介绍",
            content = "1. APP中的所有图片操作功能都依赖后端服务器，仅安装APP是没用的。\n" +
                    "2. APP中的修图、图生视频功能由ComfyUI实现，APP通过后端服务器调用ComfyUI的API进行生图。"
        ),
        HelpSection(
            title = "后端地址设置",
            content = "在顶部菜单中进入“地址设置”界面，填写完整的后端服务器地址才可使用本APP。试用地址：“http://172.93.187.227:8000/”。"
        ),
        HelpSection(
            title = "相册界面锁定功能",
            content = "1. 首次进入App后，相册界面默认不隐藏。如需开启相册锁定功能，请长按顶部左侧的标题。解锁手势不可自定义，统一设置为“Z”，在锁定界面任意处输入手势即可解锁。\n" +
                    "2. 如果需要取消相册界面锁定功能，请在锁定界面对话框中输入“unlock”并发送。"
        ),
        HelpSection(
            title = "基本功能说明",
            content = "1. 左右滑动或点击底部TAB可以切换文件夹。\n" +
                    "2. 在相册顶部下拉可以刷新当前目录。\n" +
                    "3. 顶部“+”按钮实现了图片上传和新建文件夹的功能。\n" +
                    "4. 顶部菜单按钮，“任务管理”可以监视当前ComfyUI的队列任务，“刷新页面”将清除本地缓存目录，同时服务器重新扫描当前目录，“地址设置”填写后端服务器地址，“功能设置”可隐藏不可使用的功能"
        ),
        HelpSection(
            title = "换衣功能说明",
            content = "长按图片后进入多选模式，选中若干张图片，点击浮动操作栏中的“换衣”，即可触发换衣命令。"
        ),
        HelpSection(
            title = "修图功能说明",
            content = "1. 长按图片后进入多选模式，选中若干张图片，点击浮动操作栏中的“修图”，即可触发修图命令。\n" +
                    "2. 修图的提示词可以存为模板，只需填写完提示词后点击新增即可。后续使用点击对应提示词可更新文本框中内容。\n" +
                    "3. 长按提示词模板可以修改或删除提示词。"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帮助说明", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            sections.forEach { section ->
                HelpSectionCard(section = section)
            }
        }
    }
}

data class HelpSection(
    val title: String,
    val content: String
)

@Composable
fun HelpSectionCard(section: HelpSection) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.title,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector =
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = section.content,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}
