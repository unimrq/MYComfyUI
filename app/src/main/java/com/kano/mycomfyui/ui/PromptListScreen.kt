package com.kano.mycomfyui.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kano.mycomfyui.network.ApiService.PromptItem
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kano.mycomfyui.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PromptListScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<PromptItem>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<PromptItem?>(null) }

    var searchText by remember { mutableStateOf("") } // 搜索框内容

    fun load() {
        scope.launch {
            try {
                items = RetrofitClient.getApi().getPromptList()
            } catch (e: Exception) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                Log.e("debug", e.message.toString())
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) {
                exportPromptsToJson(context, items, uri)
                Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val imported = importPromptsFromJson(context, uri)

                        val existingTitles = items.map { it.title }.toSet()

                        val uniqueToImport = imported
                            .filter { it.title.isNotBlank() }
                            .filter { it.title !in existingTitles }

                        uniqueToImport.forEach { item ->
                            RetrofitClient.getApi().addPrompt(
                                title = item.title.trim(),
                                text = item.text?.trim() ?: ""
                            )
                        }

                        load()

                        Toast.makeText(
                            context,
                            "导入 ${uniqueToImport.size} 条，跳过 ${imported.size - uniqueToImport.size} 条重复",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {
                        Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                        Log.e("debug", e.message.toString())

                    }

                }
            }
        }



    LaunchedEffect(Unit) { load() }

    val filteredItems = items.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                it.text.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.background(Color.White),
        topBar = {
            TopAppBar(
                title = { Text("提示词") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.download), contentDescription = "导入", modifier = Modifier.size(20.dp))
                    }


                    IconButton(
                        onClick = {
                            exportLauncher.launch("prompts.json")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "导出", modifier = Modifier.height(20.dp))
                    }

                    IconButton(
                        onClick = {
                            // 跳转到新增提示词页面
                            navController.navigate("prompt_add")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增提示词",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // 顶部搜索框
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("搜索提示词") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp), // ✅ 这里直接设置圆角
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.LightGray,
                            unfocusedContainerColor = Color(0xFFEAEAEA),
                            cursorColor = Color(0xFF2E7D32),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(16.dp))
                    )

                }

                items(filteredItems) { item ->
                    PromptListItem(
                        item = item,
                        onClick = {
                            navController.navigate(
                                "prompt_edit/${Uri.encode(item.title)}"
                            )
                        },
                        onLongPress = {
                            deletingItem = item
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // 删除确认弹窗保持不变
    if (showDeleteDialog && deletingItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除提示词") },
            text = { Text("确定删除「${deletingItem!!.title}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.getApi()
                                    .deletePrompt(deletingItem!!.title)
                                load()
                            } catch (e: Exception) {
                                Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                Log.e("debug", e.message.toString())
                            }
                        }
                        showDeleteDialog = false
                    }
                ) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

fun exportPromptsToJson(
    context: Context,
    prompts: List<PromptItem>,
    uri: Uri
) {
    val json = Gson().toJson(prompts)
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(json.toByteArray(Charsets.UTF_8))
    }
}

fun importPromptsFromJson(
    context: Context,
    uri: Uri
): List<PromptItem> {
    val json = context.contentResolver
        .openInputStream(uri)
        ?.bufferedReader()
        ?.readText()
        ?: return emptyList()

    val type = object : TypeToken<List<PromptItem>>() {}.type
    return Gson().fromJson(json, type)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromptListItem(
    item: PromptItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.text,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
