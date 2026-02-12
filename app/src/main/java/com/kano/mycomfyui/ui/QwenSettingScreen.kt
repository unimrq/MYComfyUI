package com.kano.mycomfyui.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

class NudePrefs(context: Context) {

    private val prefs = context.getSharedPreferences(
        "nude_params",
        Context.MODE_PRIVATE
    )

    fun get(key: String, default: String): String {
        return prefs.getString(key, default) ?: default
    }

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QwenSettingScreen(
) {
    val context = LocalContext.current
    val prefs = remember { NudePrefs(context) }

    var qwenModel by remember {
        mutableStateOf(
            prefs.get("qwenModel", "Qwen-Rapid-AIO-NSFW-v19.safetensors")
        )
    }

    var screenMode by remember {
        mutableStateOf(prefs.get("screenMode", "portrait"))
    }

    var denoise by remember {
        mutableStateOf(prefs.get("denoise", "0.85"))
    }

    var steps by remember {
        mutableStateOf(prefs.get("steps", "4"))
    }

    var samplerName by remember {
        mutableStateOf(prefs.get("samplerName", "er_sde"))
    }

    var scheduler by remember {
        mutableStateOf(prefs.get("scheduler", "beta"))
    }

    var width by remember {
        mutableStateOf(prefs.get("width", "960"))
    }

    var height by remember {
        mutableStateOf(prefs.get("height", "1440"))
    }

    var width2 by remember {
        mutableStateOf(prefs.get("width2", "1440"))
    }

    var height2 by remember {
        mutableStateOf(prefs.get("height2", "960"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("参数设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // === 模型名称 ===
            ParamTextField(
                label = "模型名称",
                value = qwenModel,
                onValueChange = {
                    qwenModel = it
                    prefs.put("qwenModel", it)
                }
            )

            // === 降噪 + 步数 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParamTextField(
                    label = "降噪 (0 ~ 1)",
                    value = denoise,
                    onValueChange = {
                        if (it.isEmpty() || it.toFloatOrNull()?.let { v -> v in 0f..1f } == true) {
                            denoise = it
                            prefs.put("denoise", it)
                        }
                    },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ParamTextField(
                    label = "步数 (4 ~ 8)",
                    value = steps,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull()?.let { v -> v in 4..8 } == true) {
                            steps = it
                            prefs.put("steps", it)
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            // === 采样器 + 调度器 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParamTextField(
                    label = "采样器",
                    value = samplerName,
                    onValueChange = {
                        samplerName = it
                        prefs.put("samplerName", it)
                    },
                    modifier = Modifier.weight(1f)
                )

                ParamTextField(
                    label = "调度器",
                    value = scheduler,
                    onValueChange = {
                        scheduler = it
                        prefs.put("scheduler", it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // === 宽度 + 高度 ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "竖屏分辨率",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp),
                        color = Color.Black
                    )

                    if (screenMode == "portrait") {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "当前为竖屏",
                            tint = Color(0xFF3965B0),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ParamTextField(
                        label = "宽度",
                        value = width,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                width = it
                                prefs.put("width", it)
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    ParamTextField(
                        label = "高度",
                        value = height,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                height = it
                                prefs.put("height", it)
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }


            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "横屏分辨率",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp),
                        color = Color.Black
                    )

                    if (screenMode == "landscape") {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "当前为竖屏",
                            tint = Color(0xFF3965B0),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ParamTextField(
                        label = "宽度",
                        value = width2,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                width2 = it
                                prefs.put("width2", it)
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    ParamTextField(
                        label = "高度",
                        value = height2,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                height2 = it
                                prefs.put("height2", it)
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        }
    }
}
