package com.kano.mycomfyui.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kano.mycomfyui.network.ApiService.PromptItem
import com.kano.mycomfyui.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlin.text.toBoolean
import androidx.core.content.edit


class EditPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nude_prefs", Context.MODE_PRIVATE)

    fun get(key: String, default: String = ""): String {
        return prefs.getString(key, default) ?: default
    }

    fun put(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditImageSheet(
    imageUrls: List<String>,
    thumbnailUrls: List<String>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs2 = remember { EditPrefs(context) }

    var promptText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // ---- 切换输入模式 ----
    var isInputMode by remember { mutableStateOf(false) }

    // ---- 标签列表 ----
    var items by remember { mutableStateOf<List<PromptItem>>(emptyList()) }
    val loadItems: suspend () -> Unit = {
        try {
            items = RetrofitClient.getApi().getPromptList()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载词条失败", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedItems by remember {
        mutableStateOf<Set<PromptItem>>(emptySet())
    }

    // ---- tag 使用次数（从 prefs 读）----
    var tagUseCount by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    // 初始化读取
    LaunchedEffect(items) {
        tagUseCount = items.associate { item ->
            val key = item.title
            item.title to (prefs2.get(key, "0").toInt())
        }
    }


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

    LaunchedEffect(Unit) {
        loadItems()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {

        // ---- 内容部分 ----
        if (isInputMode) {
            // 输入框形式
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("请输入提示词") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .focusRequester(focusRequester),
                maxLines = 5,
                singleLine = false
            )
        } else {
            // 标签列表 FlowRow
            val displayItems = remember(items) {
                items.filter { it.title.isNotBlank() }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 412.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(displayItems) { item ->
                    val selected = selectedItems.contains(item)
                    val useCount = tagUseCount[item.title] ?: 0

                    TinyTag(
                        text = item.title,
                        selected = selected,
                        useCount = useCount,
                        onClick = {
                            selectedItems =
                                if (selected) selectedItems - item else selectedItems + item
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { isInputMode = !isInputMode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3965B0)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "切换",
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



            Button(
                onClick = {
                    if (isInputMode) {
                        if (promptText.isBlank()) {
                            Toast.makeText(context, "请输入文字", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val (finalWidth, finalHeight) = if (screenMode == "portrait") {
                            width.ifBlank { "960" } to height.ifBlank { "1440" }
                        } else if (screenMode == "landscape") {
                            width2.ifBlank { "1440" } to height2.ifBlank { "960" }
                        } else {
                            "0" to "0"
                        }

                        val tagText = selectedItems.joinToString(" ") { it.text }

                        val finalText = if (tagText.isNotBlank()) {
                            "$promptText，$tagText"
                        } else {
                            promptText
                        }

                        val params = mapOf(
                            "denoise" to denoise.ifBlank { "0.85" },
                            "qwen_model" to qwenModel.ifBlank { "Qwen-Rapid-AIO-NSFW-v19.safetensors" },
                            "sampler_name" to samplerName.ifBlank { "er_sde" },
                            "scheduler" to scheduler.ifBlank { "beta" },
                            "steps" to steps.ifBlank { "4" },
                            "width" to finalWidth,
                            "height" to finalHeight,
                            "text" to finalText
                        )

                        scope.launch {
                            imageUrls.forEachIndexed { index, url ->
                                try {
                                    RetrofitClient.getApi().generateImage(
                                        type = "修图",
                                        imageUrl = url,
                                        thumbnailUrl = thumbnailUrls.getOrNull(index) ?: "",
                                        args = params
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                                }
                            }
                            onDismiss()
                        }

                        Toast.makeText(context, "已提交修图任务", Toast.LENGTH_SHORT).show()
                    } else {
                        if (selectedItems.isEmpty()) {
                            Toast.makeText(context, "请选择至少一个标签", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val (finalWidth, finalHeight) = if (screenMode == "portrait") {
                            width.ifBlank { "960" } to height.ifBlank { "1440" }
                        } else if (screenMode == "landscape") {
                            width2.ifBlank { "1440" } to height2.ifBlank { "960" }
                        } else {
                            "0" to "0"
                        }

                        // ---- 发送时：累计 tag 使用次数 ----
                        val newMap = tagUseCount.toMutableMap()

                        selectedItems.forEach { item ->
                            val key = item.title
                            val newCount = (newMap[item.title] ?: 0) + 1

                            newMap[item.title] = newCount
                            prefs2.put(key, newCount.toString())
                        }

                        tagUseCount = newMap

                        scope.launch {
                            selectedItems.forEach { prompt ->
                                val finalText = if (promptText.isNotBlank()) {
                                    promptText + "，" + prompt.text
                                } else {
                                    prompt.text
                                }

                                val params = mapOf(
                                    "denoise" to denoise.ifBlank { "0.85" },
                                    "qwen_model" to qwenModel.ifBlank { "Qwen-Rapid-AIO-NSFW-v19.safetensors" },
                                    "sampler_name" to samplerName.ifBlank { "er_sde" },
                                    "scheduler" to scheduler.ifBlank { "beta" },
                                    "steps" to steps.ifBlank { "4" },
                                    "width" to finalWidth,
                                    "height" to finalHeight,
                                    "text" to finalText,
                                    "prompt_title" to prompt.title
                                )


                                imageUrls.forEachIndexed { index, url ->
                                    try {
                                        RetrofitClient.getApi().generateImage(
                                            type = "修图",
                                            imageUrl = url,
                                            thumbnailUrl = thumbnailUrls.getOrNull(index) ?: "",
                                            args = params
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
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB3424A)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "发送",
                    color = Color.White
                )
            }
        }
    }
}


@Composable
fun TinyTag(
    text: String,
    selected: Boolean,
    useCount: Int,
    onClick: () -> Unit
) {
    val maxCount = 30f
    val fraction = (useCount / maxCount).coerceIn(0f, 1f)

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val deepColor = MaterialTheme.colorScheme.primary

    val bgColor =
        if (selected) deepColor
        else lerp(baseColor, deepColor, fraction)

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) Color.White else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}




