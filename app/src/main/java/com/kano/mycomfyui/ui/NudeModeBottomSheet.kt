package com.kano.mycomfyui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.sp


@Composable
fun ParamTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Blue,     // ✅ 去掉聚焦下划线
            unfocusedIndicatorColor = Color.Black,   // ✅ 去掉未聚焦下划线
            disabledIndicatorColor = Color.Red,    // ✅ 去掉禁用下划线
            errorIndicatorColor = Color.Red,       // ✅ 去掉错误下划线
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudeModeBottomSheet(
    onDismiss: () -> Unit,
    onCreativeModeClick: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { NudePrefs(context) }

    var denoise by remember {
        mutableStateOf(prefs.get("denoise", "0.85"))   // 0~1
    }

    var isPortrait by remember {
        mutableStateOf(
            prefs.get("isPortrait", "true").toBoolean()
        )
    }

    var qwenModel by remember {
        mutableStateOf(
            prefs.get(
                "qwenModel",
                "Qwen-Rapid-AIO-NSFW-v19.safetensors"
            )
        )
    }

    var samplerName by remember {
        mutableStateOf(prefs.get("samplerName", "er_sde"))
    }

    var scheduler by remember {
        mutableStateOf(prefs.get("scheduler", "beta"))
    }

    var steps by remember {
        mutableStateOf(prefs.get("steps", "4"))        // 4~8
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


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom =16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // === 左侧：Radio（上下排） ===
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isPortrait,
                        onClick = {
                            isPortrait = true
                            prefs.put("isPortrait", "true")
                        }
                    )
                    Text("竖屏")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isPortrait,
                        onClick = {
                            isPortrait = false
                            prefs.put("isPortrait", "false")
                        }
                    )
                    Text("横屏")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // === 右侧：发送按钮 ===
            Button(
                onClick = {
                    val (finalWidth, finalHeight) = if (isPortrait) {
                        width.ifBlank { "960" } to height.ifBlank { "1440" }
                    } else {
                        width2.ifBlank { "1440" } to height2.ifBlank { "960" }
                    }

                    val params = mapOf(
                        "denoise" to denoise.ifBlank { "0.85" },
                        "qwen_model" to qwenModel.ifBlank { "Qwen-Rapid-AIO-NSFW-v19.safetensors" },
                        "sampler_name" to samplerName.ifBlank { "er_sde" },
                        "scheduler" to scheduler.ifBlank { "beta" },
                        "steps" to steps.ifBlank { "4" },
                        "width" to finalWidth,
                        "height" to finalHeight,
                    )

                    onCreativeModeClick(params)
                },
                modifier = Modifier
                    .height(72.dp)
                    .widthIn(min = 168.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xffb3424a)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始脱衣", fontSize = 16.sp)
            }
        }


    }
}

