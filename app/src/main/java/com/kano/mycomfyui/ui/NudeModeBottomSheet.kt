package com.kano.mycomfyui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = maxLines == 1,   // 👈 关键
        minLines = 1,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Blue,
            unfocusedIndicatorColor = Color.Black,
            disabledIndicatorColor = Color.Red,
            errorIndicatorColor = Color.Red,
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

    var screenMode by remember {
        mutableStateOf(prefs.get("screenMode", "portrait"))
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

//    var promptText by remember { mutableStateOf("高质量，专业数码摄影，保持人脸一致性，保持肤色不变，保持构图不变，脱掉女生的所有衣物，不改变女生姿势") }
    var promptText by remember { mutableStateOf(prefs.get("prompt1", "")) }

    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Box(
            modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
        ){
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("请输入提示词") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .focusRequester(focusRequester),
                maxLines = 5,
                singleLine = false
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3965B0)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "收起",
                    color = Color.White
                )
            }

            SingleChoiceSegmentedButtonRow {

                val options = listOf(
                    "portrait" to "竖屏",
                    "original" to "原图",
                    "landscape" to "横屏"
                )

                options.forEachIndexed { index, (value, label) ->

                    SegmentedButton(
                        selected = screenMode == value,
                        onClick = {
                            screenMode = value
                            prefs.put("screenMode", value)
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),

                        // 👇 关键：自定义颜色
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        ),

                        // 👇 关键：去掉 icon
                        icon = {}
                    ) {
                        Text(label)
                    }
                }
            }

            // === 右侧：发送按钮 ===
            Button(
                onClick = {
                    val (finalWidth, finalHeight) = if (screenMode == "portrait") {
                        width.ifBlank { "960" } to height.ifBlank { "1440" }
                    } else if (screenMode == "landscape") {
                        width2.ifBlank { "1440" } to height2.ifBlank { "960" }
                    } else {
                        "0" to "0"
                    }

                    val params = mapOf(
                        "denoise" to denoise.ifBlank { "0.85" },
                        "qwen_model" to qwenModel.ifBlank { "Qwen-Rapid-AIO-NSFW-v19.safetensors" },
                        "sampler_name" to samplerName.ifBlank { "er_sde" },
                        "scheduler" to scheduler.ifBlank { "beta" },
                        "steps" to steps.ifBlank { "4" },
                        "width" to finalWidth,
                        "height" to finalHeight,
                        "text" to promptText
                    )

                    onCreativeModeClick(params)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xffb3424a)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("发送")
            }
        }


    }
}

