package com.kano.mycomfyui.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageSheet(
    imageUrls: List<String>,
    thumbnailUrls: List<String>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var promptText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ✅ 自动聚焦并显示输入法
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ 用 Box 叠放输入框和右下角发送按钮
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    label = { Text("请输入提示词") },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = 5
                )

                // ✅ 右下角发送按钮
                TextButton(
                    onClick = {
                        if (promptText.isBlank()) {
                            Toast.makeText(context, "请输入提示词", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        scope.launch {
                            imageUrls.forEachIndexed { index, url ->
                                try {
                                    val response = RetrofitClient.getApi().generateImage(
                                        type = "修图",
                                        imageUrl = url,
                                        thumbnailUrl = thumbnailUrls.getOrNull(index) ?: "",
                                        args = mapOf("text" to promptText)
                                    )

                                    val resultText = response.string()
                                    if (!resultText.contains("success", ignoreCase = true)) {
                                        Toast.makeText(context, "修图失败: $url", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "网络错误: $url", Toast.LENGTH_SHORT).show()
                                }
                            }

                            Toast.makeText(context, "修图任务已提交…", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .height(72.dp)
                ) {
                    Text(
                        text = "发送",
                        color = Color(0xFF224B8F)
                    )

                }
            }
        }
    }
}

