import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.kano.mycomfyui.R
import com.kano.mycomfyui.network.ServerConfig


@Composable
fun VideoDetailScreen(
    currentFile: String,
    onDismiss: () -> Unit

) {
    Box(modifier = Modifier.fillMaxSize()) {
        CachedVideoPlayer(
            currentFile.toString(),
            secretKey = ServerConfig.secret,
            modifier = Modifier.fillMaxSize(),
            onDismiss = onDismiss
        )
    }
}


@OptIn(UnstableApi::class)
@SuppressLint("RememberReturnType")
@Composable
fun CachedVideoPlayer(
    videoPath: String,
    secretKey: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var offsetY by remember { mutableStateOf(0f) }

    val fixedPath = if (videoPath.contains("/photos/")) {
        videoPath.replaceFirst("/photos/", "/videos/")
    } else {
        videoPath
    }

    val exoPlayer = remember(fixedPath) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("X-Secret" to secretKey))
        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setMediaItem(MediaItem.fromUri(fixedPath))
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // 播放状态
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0L) }

    // 更新进度
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.duration > 0) {
                progress = exoPlayer.currentPosition / exoPlayer.duration.toFloat()
                duration = exoPlayer.duration
            }
            kotlinx.coroutines.delay(200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 0) offsetY += dragAmount.y
                    },
                    onDragEnd = {
                        if (offsetY > 120f) onDismiss() else offsetY = 0f
                    }
                )
            }
    ) {
        // 视频播放器
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // 隐藏默认控制器
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 控制条背景
        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
                .zIndex(1f) // 保证在 PlayerView 之上

        ) {
            // 水平排列控件
//            androidx.compose.foundation.layout.Row(
//                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
//                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                // 快退 10 秒
//                androidx.compose.material3.IconButton(onClick = {
//                    val newPos = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
//                    exoPlayer.seekTo(newPos)
//                }) {
//                    Icon(
//                        painter = painterResource(R.drawable.rewind),
//                        contentDescription = "快进",
//                    )
//                }
//
//                // 播放/暂停
//                androidx.compose.material3.IconButton(onClick = {
//                    isPlaying = !isPlaying
//                    exoPlayer.playWhenReady = isPlaying
//                }) {
//                    androidx.compose.material3.Icon(
//                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
//                        contentDescription = "播放/暂停"
//                    )
//                }
//
//                // 快进 10 秒
//                androidx.compose.material3.IconButton(onClick = {
//                    val newPos =
//                        (exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration)
//                    exoPlayer.seekTo(newPos)
//                }) {
//                    Icon(
//                        painter = painterResource(R.drawable.rewind),
//                        contentDescription = "快进",
//                        modifier = Modifier.graphicsLayer {
//                            scaleX = -1f // 水平翻转
//                        }
//                    )
//                }
//            }

            // 进度条
            androidx.compose.material3.Slider(
                value = progress,
                onValueChange = { newProgress ->
                    progress = newProgress
                    val seekPosition = (newProgress * exoPlayer.duration).toLong()
                    exoPlayer.seekTo(seekPosition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
    }
}


