package com.kano.mycomfyui.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.kano.mycomfyui.network.ApiService.PromptItem
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

    LaunchedEffect(Unit) {
        loadItems()
    }

    // 新增弹窗状态
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }

    // 编辑弹窗状态
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    keyboardController?.hide()
                    showList = !showList
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (showList) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = "切换列表",
                        modifier = Modifier.size(24.dp).padding(0.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (showList) "收缩" else "展开")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 新增按钮 -> 弹出新增弹窗
            TextButton(
                onClick = {
                    newTitle = ""
                    newText = promptText
                    showAddDialog = true
                }
            ) {
                Text("新增", color = Color(0xFF3965B0))
            }

            Spacer(modifier = Modifier.weight(1f))

            // 发送逻辑保持不变
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
                        onDismiss()
                    }
                    Toast.makeText(context, "修图任务已提交…", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("发送", color = Color(0xFF3965B0))
            }
        }

        if (showList) {
            // 快捷词条列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(2.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { promptText = item.text }, // 点击只填充内容
                                onLongClick = {
                                    editingIndex = index
                                    editingTitle = item.title
                                    editingText = item.text
                                    showEditDialog = true
                                }
                            )
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title, // 标题上方显示
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF224B8F)
                            )
                            Text(
                                text = item.text, // 内容下方显示
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Divider()
                }
            }
        }

    }

    // 新增弹窗
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新增快捷词条") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("标题") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank() && newText.isNotBlank()) {
                            scope.launch {
                                try {
                                    RetrofitClient.getApi().addPrompt(newTitle, newText)
                                    loadItems()
                                    Toast.makeText(context, "新增成功", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "新增失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "标题和内容不能为空", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false }
                ) { Text("取消") }
            }
        )
    }

    // 编辑/删除弹窗
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改或删除") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editingTitle,
                        onValueChange = { editingTitle = it },
                        label = { Text("修改标题") }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        label = { Text("修改内容") },
                        minLines = 8,       // 至少显示8行
                        maxLines = 10,      // 可选：限制最多行数
                        singleLine = false
                    )
                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingIndex?.let { index ->
                            scope.launch {
                                try {
                                    val oldTitle = items[index].title
                                    RetrofitClient.getApi().updatePrompt(
                                        oldTitle,
                                        editingTitle,
                                        editingText
                                    )
                                    loadItems()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "修改失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showEditDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingIndex?.let { index ->
                            scope.launch {
                                try {
                                    RetrofitClient.getApi().deletePrompt(items[index].title)
                                    loadItems()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        showEditDialog = false
                    }
                ) { Text("删除", color = Color.Red) }
            }
        )
    }
}

