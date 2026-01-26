package com.kano.mycomfyui.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kano.mycomfyui.network.ApiService.PromptItem
import com.kano.mycomfyui.network.RetrofitClient
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
    var showList by remember { mutableStateOf(true) }  // 控制列表显示

    // ---- 从 API 加载词条 ----
    var items by remember { mutableStateOf<List<PromptItem>>(emptyList()) }
    val loadItems: suspend () -> Unit = {
        try {
            items = RetrofitClient.getApi().getPromptList() // 返回 PromptItem(title, text)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载词条失败", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedItems by remember {
        mutableStateOf<Set<PromptItem>>(emptySet())
    }


    LaunchedEffect(Unit) {
        loadItems()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 内容输入框
        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("请输入提示词") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState: FocusState ->
                    showList = !focusState.isFocused
                },
            maxLines = 5,
            singleLine = false
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { showList = !showList }
            ) {
                Text(if (showList) "收起" else "展开", color = Color(0xFF3965B0))
            }

            // 发送逻辑保持不变
            TextButton(
                onClick = {
                    if (selectedItems.isEmpty()) {
                        Toast.makeText(context, "请选择至少一个标签", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    scope.launch {
                        selectedItems.forEach { prompt ->
                            imageUrls.forEachIndexed { index, url ->
                                try {
                                    RetrofitClient.getApi().generateImage(
                                        type = "修图",
                                        imageUrl = url,
                                        thumbnailUrl = thumbnailUrls.getOrNull(index) ?: "",
                                        args = mapOf("text" to (prompt.text ?: ""))
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        onDismiss()
                    }

                    Toast.makeText(
                        context,
                        "已提交 ${selectedItems.size} 个提示词的修图任务",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("发送", color = Color(0xFF3965B0))
            }

        }

        if (showList) {
            val displayItems = remember(items) {
                items.filter { it.title.isNotBlank() }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 6.dp,
                        alignment = Alignment.CenterHorizontally // 👈 关键
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    displayItems.forEach { item ->
                        val selected = selectedItems.contains(item)

                        TinyTag(
                            text = item.title ?: "",
                            selected = selected,
                            onClick = {
                                selectedItems =
                                    if (selected) {
                                        selectedItems - item
                                    } else {
                                        selectedItems + item
                                    }
                            }
                        )
                    }

                }
            }
        }
    }


}

@Composable
fun TinyTag(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = if (selected) Color.White else Color.DarkGray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}



