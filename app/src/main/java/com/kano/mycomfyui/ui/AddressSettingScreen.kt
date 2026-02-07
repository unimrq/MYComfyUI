package com.kano.mycomfyui.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import kotlinx.coroutines.launch

private const val PREFS_NAME = "app_settings"
private const val KEY_ADDRESS_LIST = "address_list"
private const val KEY_SELECTED_ADDRESS = "selected_address"
private const val MAX_ADDRESS_COUNT = 3

data class ServerAddress(
    val address: String = "",   // host:port
    val scheme: String = "http://",
    val secret: String = ""
) {
    fun buildBaseUrl(): String? {
        val raw = address.trim()
        if (raw.isEmpty()) return null

        val clean = raw
            .removePrefix("http://")
            .removePrefix("https://")

        val parts = clean.split(":")

        val host: String
        val port: Int?

        when (parts.size) {
            1 -> {
                // 没写端口
                if (scheme == "http://") return null   // HTTP 必须写端口
                host = parts[0]
                port = null // HTTPS 默认 443
            }
            2 -> {
                host = parts[0]
                port = parts[1].toIntOrNull() ?: return null
                if (port !in 1..65535) return null
            }
            else -> return null
        }

        return if (port != null) {
            "$scheme$host:$port/"
        } else {
            "$scheme$host/"
        }
    }

}



// 保存地址列表（JSON 数组形式）
fun saveAddressList(context: Context, list: List<ServerAddress>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = Gson().toJson(list)
    prefs.edit().putString(KEY_ADDRESS_LIST, json).apply()
}


// 读取地址列表
fun loadAddressList(context: Context): List<ServerAddress> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_ADDRESS_LIST, null)

    val list = if (json.isNullOrEmpty()) {
        emptyList()
    } else {
        runCatching {
            Gson().fromJson(json, Array<ServerAddress>::class.java).toList()
        }.getOrElse { emptyList() }
    }

    return (list + List(MAX_ADDRESS_COUNT) { ServerAddress() })
        .take(MAX_ADDRESS_COUNT)
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
    var selectedIndex by remember { mutableStateOf(
        getSavedAddress(context)?.toIntOrNull()
    ) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地址设置") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            addressList.forEachIndexed { index, item ->
                AddressCard(
                    index = index,
                    data = item,
                    selected = selectedIndex == index,
                    onSelect = {
                        val baseUrl = item.buildBaseUrl()
                        if (baseUrl == null) {
                            Toast
                                .makeText(context, "地址或端口无效", Toast.LENGTH_SHORT)
                                .show()
                            return@AddressCard
                        }

                        selectedIndex = index
                        saveAddress(context, index.toString())

                        ServerConfig.baseUrl = baseUrl
                        RetrofitClient.rebuildRetrofit()

                        Toast
                            .makeText(context, "已切换地址，请重启 App", Toast.LENGTH_SHORT)
                            .show()
                    }
                    ,
                    onChange = { updated ->
                        addressList = addressList.toMutableList().apply {
                            set(index, updated)
                        }
                        coroutineScope.launch {
                            saveAddressList(context, addressList)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddressCard(
    index: Int,
    data: ServerAddress,
    selected: Boolean,
    onSelect: () -> Unit,
    onChange: (ServerAddress) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                Color(0xFFE3F2FD)
            else
                Color(0xFFF8F8F8)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "地址 ${index + 1}",
                    fontSize = 16.sp,
                    color = Color(0xFF3965B0),
                    modifier = Modifier.padding(start = 0.dp,end = 8.dp)
                )
                // ===== 地址是否启用 =====
                Switch(
                    checked = selected,
                    onCheckedChange = { if (it) onSelect() },
                    modifier = Modifier.scale(0.8f)
                )

            }


//            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(85.dp)
                        .height(32.dp)
                        .clickable {
                            val newScheme = if (data.scheme == "https://") "http://" else "https://"
                            onChange(data.copy(scheme = newScheme))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.scheme,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }


                TextField(
                    value = data.address,
                    onValueChange = { onChange(data.copy(address = it)) },
                    placeholder  = { Text("服务器地址", fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,     // ✅ 去掉聚焦下划线
                        unfocusedIndicatorColor = Color.Transparent,   // ✅ 去掉未聚焦下划线
                        disabledIndicatorColor = Color.Transparent,    // ✅ 去掉禁用下划线
                        errorIndicatorColor = Color.Transparent,       // ✅ 去掉错误下划线
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("密钥：", modifier = Modifier.width(85.dp),textAlign = TextAlign.Center, fontSize = 14.sp)
                TextField(
                    value = data.secret,
                    onValueChange = { onChange(data.copy(secret = it)) },
                    placeholder  = { Text("无密钥可留空", fontSize = 14.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,     // ✅ 去掉聚焦下划线
                        unfocusedIndicatorColor = Color.Transparent,   // ✅ 去掉未聚焦下划线
                        disabledIndicatorColor = Color.Transparent,    // ✅ 去掉禁用下划线
                        errorIndicatorColor = Color.Transparent,       // ✅ 去掉错误下划线
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }



        }
    }
}



