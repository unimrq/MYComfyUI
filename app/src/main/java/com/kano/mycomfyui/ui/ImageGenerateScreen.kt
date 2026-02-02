package com.kano.mycomfyui.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerateBottomSheet(
    imageUrls: List<String>,           // ✅ 多张图片
    thumbnailUrls: List<String>,       // ✅ 对应的缩略图
    onDismiss: () -> Unit
) {
    var selectedTemplate by remember { mutableStateOf("") }
    var gifTemplates by remember { mutableStateOf(listOf<String>()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 拉取模板
    LaunchedEffect(Unit) {
        try {
            gifTemplates = RetrofitClient.getApi().getGifTemplates()
            if (gifTemplates.isNotEmpty()) selectedTemplate = gifTemplates.first()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 模板选择按钮网格，占用剩余空间
            if (gifTemplates.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gifTemplates) { template ->  // ✅ 直接传列表
                        val isSelected = template == selectedTemplate
                        Button(
                            onClick = { selectedTemplate = template },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(template, maxLines = 1)
                        }
                    }
                }

            }
        }

        // 底部右侧发送按钮
        IconButton(
            onClick = {
                scope.launch {
                    imageUrls.forEachIndexed { index, url ->
                        try {
                            val response = RetrofitClient.getApi().generateImage(
                                type = "动图",
                                imageUrl = url,
                                thumbnailUrl = thumbnailUrls.getOrNull(index) ?: "",
                                args = mapOf("text" to selectedTemplate)
                            )

                            val resultText = response.string()
                            if (!resultText.contains("success", ignoreCase = true)) {
                                Toast.makeText(context, "生成失败: $url", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "网络错误: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Toast.makeText(context, "正在生成动图…", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd) // 最底端右侧
                .padding(8.dp)
                .width(80.dp)
        ) {
            Text(
                text = "发送",
                color = Color(0xFF224B8F),
                fontSize = 14.sp
            )
        }
    }


}
