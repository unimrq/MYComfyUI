package com.kano.mycomfyui.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.launch
import org.json.JSONArray

private const val PREFS_NAME = "app_settings"
private const val KEY_ADDRESS_LIST = "address_list"
private const val KEY_SELECTED_ADDRESS = "selected_address"

// 保存地址列表（JSON 数组形式）
fun saveAddressList(context: Context, list: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = JSONArray(list).toString()
    prefs.edit().putString(KEY_ADDRESS_LIST, json).apply()
}

// 读取地址列表
fun loadAddressList(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_ADDRESS_LIST, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

// 保存当前选中地址
fun saveAddress(context: Context, address: String?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_SELECTED_ADDRESS, address).apply()
}

// 获取当前选中地址
fun getSavedAddress(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_SELECTED_ADDRESS, null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSettingScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var addressList by remember { mutableStateOf(loadAddressList(context)) }
    var selectedAddress by remember { mutableStateOf(getSavedAddress(context)) }

    var showDialog by remember { mutableStateOf(false) }
    var newAddress by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地址设置", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加地址",
                            tint = Color.Black
                        )
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (addressList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无地址，请点击右上角 + 添加")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(addressList) { index, address ->
                        val isSelected = selectedAddress == address

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAddress = address
                                    coroutineScope.launch { saveAddress(context, selectedAddress) }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    Color(0xFFE3F2FD)
                                else
                                    Color(0xFFF8F8F8)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedAddress = address

                                            // 更新全局地址并重建 Retrofit
                                            ServerConfig.baseUrl = selectedAddress ?: "http://192.168.1.1:8000/"
                                            RetrofitClient.rebuildRetrofit()

                                            // 保存到本地
                                            coroutineScope.launch {
                                                saveAddress(context, selectedAddress)
                                            }
                                        } else {
                                            selectedAddress = null
                                            coroutineScope.launch {
                                                saveAddress(context, null)
                                            }
                                        }
                                    }
                                )
                                Text(
                                    text = address,
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    val updated = addressList.toMutableList().apply { removeAt(index) }
                                    addressList = updated
                                    coroutineScope.launch { saveAddressList(context, updated) }
                                    if (selectedAddress == address) {
                                        selectedAddress = null
                                        coroutineScope.launch { saveAddress(context, null) }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 🔹 添加地址的对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("新增访问地址") },
            text = {
                OutlinedTextField(
                    value = newAddress,
                    onValueChange = { newAddress = it },
                    label = { Text("请输入完整地址，例：http://192.168.1.1:8000/") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newAddress.text.trim()
                    if (trimmed.isNotEmpty() && trimmed !in addressList) {
                        addressList = addressList + trimmed
                        coroutineScope.launch { saveAddressList(context, addressList) }
                        newAddress = TextFieldValue("")
                    }
                    showDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

