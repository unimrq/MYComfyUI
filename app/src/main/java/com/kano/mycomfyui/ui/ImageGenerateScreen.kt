package com.kano.mycomfyui.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
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
            .height(200.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 模板选择按钮行
            if (gifTemplates.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    mainAxisSpacing = 4.dp,
                    crossAxisSpacing = 8.dp,
                    mainAxisAlignment = MainAxisAlignment.Center
                ) {
                    gifTemplates.forEachIndexed { index, template ->
                        val isSelected = template == selectedTemplate
                        Button(
                            onClick = { selectedTemplate = template },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text(template, maxLines = 1)
                        }
                    }

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
                        modifier = Modifier.width(80.dp)  // 设置按钮的大小
                    ) {
                        Text(
                            text = "发送",
                            color = Color(0xFF224B8F),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
