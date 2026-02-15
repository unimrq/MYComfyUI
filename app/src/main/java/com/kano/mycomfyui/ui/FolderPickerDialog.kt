package com.kano.mycomfyui.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kano.mycomfyui.R
import com.kano.mycomfyui.network.RetrofitClient


@Composable
fun FolderPickerDialog(
    initialPath: String = ".",
    onCancel: () -> Unit,
    onConfirm: (selectedPath: String) -> Unit
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    var folderList by remember { mutableStateOf(listOf<Folder>()) }
    val context = LocalContext.current

    // 获取文件夹列表
    suspend fun loadFolders(path: String) {
        try {
//            Log.d("Folders", "path: $path")
            val response = RetrofitClient.getApi().getFolders(parent_path = path)
            folderList = response
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "获取文件夹失败", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentPath) {
        loadFolders(currentPath)
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
        ) {
            Column {
                // 顶部标题
                Text(
                    text = "剪切到",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 4.dp)
                )

                // 地址栏
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8E8E8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pathParts =
                            if (currentPath == ".") listOf(".") else currentPath.trim('/').split("/").filter { it.isNotEmpty() }
                        val displayedParts = if (pathParts.size > 3) pathParts.takeLast(3) else pathParts

                        displayedParts.forEachIndexed { index, part ->
                            Text(
                                text = part,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    val newPath = if (currentPath == ".") "."
                                    else "/" + pathParts.take(index + pathParts.size - displayedParts.size + 1)
                                        .joinToString("/")
                                    currentPath = newPath
                                }
                            )
                            if (index != displayedParts.lastIndex) Text(" / ")
                        }
                    }
                }

                // 文件夹列表 + loading
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn {
                        // 上一级
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (currentPath != ".") {
                                            val parent = currentPath.substringBeforeLast("/", ".")
                                            currentPath = if (parent.isEmpty()) "." else parent
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.folder),
                                    contentDescription = "上一级",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(Modifier.width(16.dp))
                                Text("上一级")
                            }
                        }


                        // 文件夹列表
                        items(folderList) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPath = folder.path }
                                    .padding(vertical = 6.dp, horizontal = 16.dp), // 缩小垂直间距
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.folder),
                                    contentDescription = "文件夹",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(folder.name)
                            }
                        }
                    }

                }

                // 取消/确定
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(currentPath) }) { Text("确定") }
                }
            }
        }
    }
}

// Folder 数据类
data class Folder(
    val name: String = "",
    val path: String = "",
    val parent_path: String = ""
)