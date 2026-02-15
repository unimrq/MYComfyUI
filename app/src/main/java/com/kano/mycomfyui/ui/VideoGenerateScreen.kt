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
import com.kano.mycomfyui.network.ApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerateBottomSheet(
    imageUrls: List<String>,
    thumbnailUrls: List<String>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var isInputMode by remember { mutableStateOf(false) }

    // ---- 标签列表 ----
    var items by remember { mutableStateOf<List<ApiService.WanPromptItem>>(emptyList()) }
    val loadItems: suspend () -> Unit = {
        try {
            items = RetrofitClient.getApi().getWanPromptList()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载词条失败", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedItems by remember {
        mutableStateOf<Set<ApiService.WanPromptItem>>(emptySet())
    }

    // ---- tag 使用次数（从 prefs 读）----
    var tagUseCount by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    LaunchedEffect(Unit) {
        loadItems()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
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

            Button(
                onClick = {

                    scope.launch {

                        if (!isInputMode && selectedItems.isEmpty()) {
                            Toast.makeText(context, "请选择至少一个标签", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (isInputMode && promptText.isBlank()) {
                            Toast.makeText(context, "请输入提示词", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val api = RetrofitClient.getApi()

                        if (isInputMode) {
                            // 🔥 输入模式：只提交一次（每张图片一次）
                            imageUrls.forEachIndexed { index, imageUrl ->

                                val thumbnailUrl = thumbnailUrls.getOrNull(index) ?: ""

                                try {
                                    api.generateImage(
                                        type = "动图",
                                        imageUrl = imageUrl,
                                        thumbnailUrl = thumbnailUrl,
                                        args = mapOf(
                                            "text" to "free",
                                            "prompt" to promptText,
                                            "prompt_title" to "自由编辑"
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        } else {
                            // 🔥 标签模式：按原逻辑
                            selectedItems.forEach { tag ->
                                imageUrls.forEachIndexed { index, imageUrl ->

                                    val thumbnailUrl = thumbnailUrls.getOrNull(index) ?: ""

                                    try {
                                        api.generateImage(
                                            type = "动图",
                                            imageUrl = imageUrl,
                                            thumbnailUrl = thumbnailUrl,
                                            args = mapOf(
                                                "text" to tag.title,
                                                "prompt_title" to tag.title
                                            )
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }

                        Toast.makeText(
                            context,
                            "任务已提交",
                            Toast.LENGTH_SHORT
                        ).show()

                        onDismiss()
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