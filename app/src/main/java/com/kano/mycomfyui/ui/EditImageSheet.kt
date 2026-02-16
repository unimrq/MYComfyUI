package com.kano.mycomfyui.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.navigation.NavController


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
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs2 = remember { EditPrefs(context) }

//    var promptText by remember { mutableStateOf("高质量，专业数码摄影，保持人脸一致性，保持肤色不变，") }

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

    var promptText by remember { mutableStateOf(prefs.get("prompt2", "")) }


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
                        },
                        onLongClick = {
                            onDismiss()
                            navController.navigate("prompt_edit/${item.title}")
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
                            activeContainerColor = Color(0xFF3965B0),
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

                    // ---------- 1️⃣ 基础校验 ----------
                    if (isInputMode && promptText.isBlank()) {
                        Toast.makeText(context, "请输入文字", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!isInputMode && selectedItems.isEmpty()) {
                        Toast.makeText(context, "请选择至少一个标签", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // ---------- 2️⃣ 计算尺寸 ----------
                    val (finalWidth, finalHeight) = when (screenMode) {
                        "portrait" -> width.ifBlank { "960" } to height.ifBlank { "1440" }
                        "landscape" -> width2.ifBlank { "1440" } to height2.ifBlank { "960" }
                        else -> "0" to "0"
                    }

                    // ---------- 3️⃣ 生成最终要发送的 prompt 列表 ----------
                    val finalPrompts = mutableListOf<Pair<String, String?>>()
                    // Pair<text, prompt_title?>

                    when {
                        // ① 有输入框 + 有标签
                        promptText.isNotBlank() && selectedItems.isNotEmpty() -> {
                            selectedItems.forEach { tag ->
                                finalPrompts.add(
                                    "$promptText${tag.text}" to tag.title
                                )
                            }
                        }

                        // ② 只有输入框
                        promptText.isNotBlank() -> {
                            finalPrompts.add(promptText to null)
                        }

                        // ③ 只有标签
                        selectedItems.isNotEmpty() -> {
                            selectedItems.forEach { tag ->
                                finalPrompts.add(tag.text to tag.title)
                            }
                        }
                    }

                    // ---------- 4️⃣ 统计标签使用次数 ----------
                    if (selectedItems.isNotEmpty()) {
                        val newMap = tagUseCount.toMutableMap()

                        selectedItems.forEach { item ->
                            val key = item.title
                            val newCount = (newMap[key] ?: 0) + 1
                            newMap[key] = newCount
                            prefs2.put(key, newCount.toString())
                        }

                        tagUseCount = newMap
                    }

                    // ---------- 5️⃣ 统一发送 ----------
                    scope.launch {

                        finalPrompts.forEach { (text, promptTitle) ->

                            val params = mutableMapOf(
                                "denoise" to denoise.ifBlank { "0.85" },
                                "qwen_model" to qwenModel.ifBlank { "Qwen-Rapid-AIO-NSFW-v19.safetensors" },
                                "sampler_name" to samplerName.ifBlank { "er_sde" },
                                "scheduler" to scheduler.ifBlank { "beta" },
                                "steps" to steps.ifBlank { "4" },
                                "width" to finalWidth,
                                "height" to finalHeight,
                                "text" to text
                            )

                            // 只有有标题才传
                            promptTitle?.let {
                                params["prompt_title"] = it
                            }

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

                    // ---------- 6️⃣ Toast 提示 ----------
                    Toast.makeText(
                        context,
                        "已提交 ${finalPrompts.size} 个修图任务",
                        Toast.LENGTH_SHORT
                    ).show()
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
    useCount: Int,
    selected: Boolean,
    maxCount: Int = 30,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null // 新增长按回调，可选
) {
    val fraction = (useCount.toFloat() / maxCount).coerceIn(0f, 1f)

    val deepColor = Color(0xFFB5B5B7)
    val lightColor = Color(0xFFE8E8EB)

    val bgColor = if (selected) {
        Color(0xFFB22222)
    } else {
        lerp(lightColor, deepColor, fraction)
    }

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .combinedClickable(   // 使用 combinedClickable 支持长按和点击
                onClick = onClick,
                onLongClick = onLongClick
            )
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



