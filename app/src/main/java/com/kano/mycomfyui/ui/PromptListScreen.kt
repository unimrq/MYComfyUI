package com.kano.mycomfyui.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kano.mycomfyui.network.ApiService.PromptItem
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptListScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<PromptItem>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingItem by remember { mutableStateOf<PromptItem?>(null) }

    fun load() {
        scope.launch {
            try {
                items = RetrofitClient.getApi().getPromptList()
            } catch (e: Exception) {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        modifier = Modifier.background(Color.White),
        topBar = {
            TopAppBar(
                title = { Text("提示词") },
                actions = {
                    IconButton(
                        onClick = {
                            // 跳转到新增提示词页面
                            navController.navigate("prompt_add")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增提示词"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
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

    // 删除确认弹窗
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
                text = item.text ?: "",
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
