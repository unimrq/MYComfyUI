package com.kano.mycomfyui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kano.mycomfyui.network.RetrofitClient
import com.kano.mycomfyui.network.ServerConfig
import com.kano.mycomfyui.ui.AlbumScreen
import com.kano.mycomfyui.ui.AddressSettingScreen
import com.kano.mycomfyui.ui.PerspectiveScreen
import com.kano.mycomfyui.ui.PromptAddScreen
import com.kano.mycomfyui.ui.PromptEditScreen
import com.kano.mycomfyui.ui.PromptListScreen
import com.kano.mycomfyui.ui.QwenSettingScreen
import com.kano.mycomfyui.ui.SettingsScreen
import com.kano.mycomfyui.ui.TaskScreen
import com.kano.mycomfyui.ui.loadAddressList
import com.kano.mycomfyui.ui.theme.MYComfyUITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs


class MainActivity : ComponentActivity() {

    private var backPressedTime = 0L

    @SuppressLint("UnrememberedMutableState")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val index = prefs.getString("selected_address", null)?.toIntOrNull()


        if (index != null) {
            val list = loadAddressList(this)
            val address = list.getOrNull(index)
            val baseUrl = address?.buildBaseUrl()
            val secret = address?.secret.toString().trim()

            if (baseUrl != null) {
                ServerConfig.baseUrl = baseUrl
                ServerConfig.secret = secret
                RetrofitClient.rebuildRetrofit()
            } else {
                ServerConfig.baseUrl = "http://192.168.1.1:8000/"
            }
        } else {
            ServerConfig.baseUrl = "http://192.168.1.1:8000/"
        }

//        Log.d("Addr", ServerConfig.baseUrl)
//        Log.d("Addr", ServerConfig.secret)

        RetrofitClient.rebuildRetrofit()

        fun saveUnlockState(context: Context, unlocked: Boolean) {
            val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("is_unlocked", unlocked).apply()
        }

        fun loadUnlockState(context: Context): Boolean {
            val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
            return prefs.getBoolean("is_unlocked", true)
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )

        setContent {
            MYComfyUITheme {
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()

                var topBarTitle by remember { mutableStateOf("") }
                var showSetting by remember { mutableStateOf(false) }
                var showRefresh by remember { mutableStateOf(false) }
                var onAddImageClick by remember { mutableStateOf<() -> Unit>({}) }
                var onLockClick by remember { mutableStateOf<() -> Unit>({}) }
                var onTaskManageClick by remember { mutableStateOf<() -> Unit>({}) }
                var isUnlockedPersistent by remember { mutableStateOf(loadUnlockState(this@MainActivity)) }
                var isUnlockedTemporary by remember { mutableStateOf(isUnlockedPersistent) }

                Scaffold(
                    modifier = Modifier.background(Color.White),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),

                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "album",
                        modifier = Modifier.background(Color.White)
                    ) {
                        paddingValues
                        composable("album") {
                            if (isUnlockedTemporary) {
                                AlbumScreen(
                                    onExitApp = { handleExitApp(scope) },
                                    navController = navController,
                                    onLockClick = {
                                        isUnlockedPersistent = false
                                        isUnlockedTemporary = false
                                        saveUnlockState(this@MainActivity, false)
                                    }
                                )
                            } else {
                                DeepSeekHomeScreen(
                                    onUnlock = {
                                        // 手势解锁只修改瞬时状态
                                        isUnlockedTemporary = true
                                    },
                                    onCommand = { cmd ->
                                        when (cmd.lowercase()) {
                                            "unlock" -> {
                                                isUnlockedPersistent = true
                                                isUnlockedTemporary = true
                                                saveUnlockState(this@MainActivity, true)
                                            }
                                            "lock" -> {
                                                isUnlockedPersistent = false
                                                isUnlockedTemporary = false
                                                saveUnlockState(this@MainActivity, false)
                                            }
                                            "scan" -> {
                                                scope.launch {
                                                    try {
                                                        RetrofitClient.getApi().scanFolder()
                                                        Toast.makeText(this@MainActivity, "扫描已开始", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(this@MainActivity, "扫描失败：${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            "detect" -> {
                                                scope.launch {
                                                    try {
                                                        RetrofitClient.getApi().detectFolder()
                                                        Toast.makeText(this@MainActivity, "检测已开始", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(this@MainActivity, "检测失败：${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        composable("taskManage") {
                            TaskScreen(
                                navController,
                                setTopBar = { title, refreshVisible, settingVisible, refreshClick, imageClick, taskManageClick, lockClick ->
                                    topBarTitle = title
                                    showRefresh = refreshVisible
                                    showSetting = settingVisible
                                    onAddImageClick = imageClick
                                    onTaskManageClick = taskManageClick
                                    onLockClick = lockClick
                                })
                        }

                        composable("address_settings") {
                            AddressSettingScreen()
                        }

                        composable("prompt_list") {
                            PromptListScreen(navController)
                        }

                        composable("settings") {
                            SettingsScreen(navController)
                        }

                        composable("qwen") {
                            QwenSettingScreen()
                        }


                        composable(
                            route = "prompt_edit/{title}",
                            arguments = listOf(
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title") ?: ""
                            PromptEditScreen(
                                navController = navController,
                                originalTitle = title
                            )
                        }

                        composable("prompt_add") {
                            PromptAddScreen(
                                navController = navController,
                            )
                        }

                    }
                }
            }
        }
    }

    private fun handleExitApp(scope: CoroutineScope) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            finish()
        } else {
            backPressedTime = currentTime
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            scope.launch { delay(1500) }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepSeekHomeScreen(
    onUnlock: () -> Unit,
    onCommand: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "新对话",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: 打开菜单 */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomInputBar(
                message = message,
                onMessageChange = { message = it },
                onSend = {
                    if (message.lowercase() in listOf("unlock", "lock", "scan", "detect")) {
                        onCommand(message)
                    }
                    message = ""
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.comfyui),
                    contentDescription = null,
                    tint = Color(0xFF2A7BEF),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "嗨！我是 ComfyMobile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ZGestureUnlockArea(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                onUnlock()
            }
        }
    }
}

@Composable
fun BottomInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {

            TextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("给 ComfyMobile 发送消息", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7F7F7),
                    unfocusedContainerColor = Color(0xFFF7F7F7),
                    cursorColor = Color(0xFF4A80F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* TODO: 添加附件 */ }) {
                        Icon(Icons.Default.Add, contentDescription = "添加", tint = Color.Black)
                    }
                    IconButton(onClick = { onSend() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "发送",
                            tint = Color(0xFF4A80F0),
                            modifier = Modifier.rotate(90f)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ZGestureUnlockArea(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit
) {
    // 存储手势路径点
    val pathPoints = remember { mutableStateListOf<Offset>() }

    // 画出手势轨迹（调试时可见）
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        pathPoints.clear()
                        pathPoints.add(offset)
                    },
                    onDrag = { change, _ ->
                        pathPoints.add(change.position)
                    },
                    onDragEnd = {
                        if (isZGesture(pathPoints)) {
                            onUnlock()
                        }
                        pathPoints.clear()
                    }
                )
            }
    ) {
    }
}

fun isZGesture(points: List<Offset>): Boolean {
    if (points.size < 5) return false

    val xs = points.map { it.x }
    val ys = points.map { it.y }
    val minX = xs.minOrNull() ?: return false
    val maxX = xs.maxOrNull() ?: return false
    val minY = ys.minOrNull() ?: return false
    val maxY = ys.maxOrNull() ?: return false

    val topPart = points.take(points.size / 3)
    val midPart = points.drop(points.size / 3).take(points.size / 3)
    val bottomPart = points.drop(2 * points.size / 3)

    val topSlope = slope(topPart)
    val midSlope = slope(midPart)
    val bottomSlope = slope(bottomPart)

    // 严格的斜率范围
    val horizontalThreshold = 0.5   // 上下水平段的最大斜率
    val diagonalThreshold = -1.5    // 中间斜段的斜率范围（负值，向左下倾斜）

    val topValid = abs(topSlope) <= horizontalThreshold
    val midValid = midSlope in diagonalThreshold..-0.5
    val bottomValid = abs(bottomSlope) <= horizontalThreshold

    val minDeltaX = 80
    val minDeltaY = 60

    return topValid &&
            midValid &&
            bottomValid &&
            topPart.first().x < topPart.last().x &&
            bottomPart.first().x < bottomPart.last().x &&
            (maxX - minX) >= minDeltaX &&
            (maxY - minY) >= minDeltaY
}




fun slope(points: List<Offset>): Double {
    if (points.size < 2) return 0.0
    val dx = points.last().x - points.first().x
    val dy = points.last().y - points.first().y
    return if (dx == 0f) {
        Double.POSITIVE_INFINITY
    } else {
        (dy / dx).toDouble()
    }
}



