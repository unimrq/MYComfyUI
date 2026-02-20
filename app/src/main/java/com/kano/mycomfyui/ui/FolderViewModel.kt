package com.kano.mycomfyui.ui

import androidx.lifecycle.ViewModel
import com.kano.mycomfyui.data.FileInfo
import com.kano.mycomfyui.data.FolderContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale


enum class Mode(val value: String) {

    ALL("ALL"),          // 原逻辑
    ORIGIN("ORIGIN"),    // 分组后取名称最短的一张
    NUDE("NUDE"),        // 分组后取最短且名称含 "-脱衣"
    EDIT("EDIT"),        // 只取名称含 "-修图"
    VIDEO("VIDEO"),        // 只取名称含 "-修图"
    QUALITY("QUALITY"),
    ZOOM("ZOOM");

    companion object {
        fun fromValue(value: String?): Mode {
            return entries.find { it.value == value } ?: ALL
        }
    }
}


fun sortPreviewableFiles(
    files: List<FileInfo>,
    currentPath: String,
    dateFormat: SimpleDateFormat,
    mode: Mode,
    sortMode: String // "从旧到新" 或 "从新到旧"
): List<FileInfo> {

    fun FileInfo.isPreviewable(): Boolean =
        file_url?.matches(
            Regex(".*\\.(png|jpg|jpeg|gif|mp4|bmp)$", RegexOption.IGNORE_CASE)
        ) == true

    fun getCoreName(name: String): String {
        return name
            .substringBeforeLast(".")
            .replace(Regex("-(脱衣|修图|放大|质感).*"), "")
    }

    fun FileInfo.parseTime(): Long =
        runCatching { dateFormat.parse(this.date)?.time ?: Long.MAX_VALUE }
            .getOrDefault(Long.MAX_VALUE)

    val validFiles = files
        .filter { !it.is_dir }
        .filter { it.isPreviewable() }

    // ===== 特殊情况：MP4 或 修图路径 =====
    val isMp4Only = validFiles.isNotEmpty() &&
            validFiles.all { it.file_url?.endsWith(".mp4", true) == true }

    // 根据 sortMode 决定排序函数
    val timeComparator: Comparator<FileInfo> = when (sortMode) {
        "从旧到新" -> compareBy { it.parseTime() }
        else -> compareByDescending { it.parseTime() } // 默认 "从新到旧"
    }

    if (isMp4Only || currentPath == "修图") {
        return validFiles.sortedWith(timeComparator)
    }

    // ===== 统一分组（只算一次）=====
    val grouped = validFiles.groupBy { getCoreName(it.name) }

    // ===== 原图顺序（权威顺序）=====
    val originList = grouped
        .mapNotNull { (_, group) -> group.minByOrNull { it.name.length } }
        .sortedWith(timeComparator)

    val originOrderMap = originList
        .mapIndexed { index, file -> getCoreName(file.name) to index }
        .toMap()

    return when (mode) {
        Mode.ALL -> {
            grouped
                .toList()
                .sortedByDescending { (_, group) ->
                    group.minOfOrNull { it.parseTime() } ?: Long.MAX_VALUE
                }
                .flatMap { (_, group) ->
                    group.sortedWith(
                        compareBy<FileInfo> { it.parseTime() }
                            .thenBy { it.name.lowercase() }
                            .let { comparator ->
                                if (sortMode == "从旧到新") comparator else comparator.reversed()
                            }
                    )
                }
        }

        Mode.ORIGIN -> originList

        Mode.NUDE -> {
            grouped
                .mapNotNull { (_, group) ->
                    group.filter { it.name.contains("-脱衣") }
                        .minByOrNull { it.name.length }
                }
                .sortedWith(
                    if (sortMode == "从旧到新") {
                        compareBy { it.parseTime() }
                    } else {
                        compareByDescending { it.parseTime() }
                    }
                )
        }

        Mode.EDIT -> {
            validFiles
                .filter { it.name.contains("-修图") }
                .sortedWith(timeComparator)
        }

        Mode.QUALITY -> {
            validFiles
                .filter { it.name.contains("-质感") }
                .sortedWith(timeComparator)
        }

        Mode.ZOOM -> {
            validFiles
                .filter { it.name.contains("-放大") }
                .sortedWith(timeComparator)
        }

        Mode.VIDEO -> {
            validFiles
                .filter { it.name.lowercase().endsWith(".mp4") }
                .sortedWith(timeComparator)
        }
    }
}




data class ImageViewerState(
    val currentPath: String = "",
    val folderContent: FolderContent? = null,
    val sortedFiles: List<FileInfo> = emptyList(),
    val currentIndex: Int = 0,
    val previewPath: String? = null,
    val previewVideo: String? = null,
    val selectedPaths: Set<String> = emptySet()
)


class FolderViewModel : ViewModel() {

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val _uiState = MutableStateFlow(ImageViewerState())
    val uiState: StateFlow<ImageViewerState> = _uiState

    fun deleteSingleAndUpdatePreview(file: FileInfo) {
        _uiState.update { state ->
            val newFiles = state.sortedFiles.filterNot {
                it.path == file.path || it.file_url == file.file_url
            }

            if (newFiles.isEmpty()) {
                return@update state.copy(
                    sortedFiles = emptyList(),
                    previewPath = null,
                    currentIndex = 0,
                    selectedPaths = emptySet()
                )
            }

            val newIndex = state.currentIndex
                .coerceAtMost(newFiles.lastIndex)

            val newPreviewFile = newFiles[newIndex]

            state.copy(
                sortedFiles = newFiles,
                currentIndex = newIndex,
                previewPath = newPreviewFile.net_url,
                selectedPaths = setOf(newPreviewFile.file_url ?: newPreviewFile.path)
            )
        }
    }

    fun deleteMultipleAndUpdateState(pathsToDelete: Set<String>) {
        _uiState.update { state ->

            // 1️⃣ 从 sortedFiles 中移除被删除的文件
            val newFiles = state.sortedFiles.filterNot { file ->
                val key = file.file_url ?: file.path
                key in pathsToDelete
            }

            // 2️⃣ 处理 currentIndex 越界
            val newIndex = if (newFiles.isEmpty()) {
                0
            } else {
                state.currentIndex.coerceAtMost(newFiles.lastIndex)
            }

            // 3️⃣ 处理 previewPath
            val newPreviewPath = when {
                newFiles.isEmpty() -> null
                state.previewPath == null -> null
                else -> newFiles.getOrNull(newIndex)?.net_url
            }

            state.copy(
                sortedFiles = newFiles,
                currentIndex = newIndex,
                previewPath = newPreviewPath,
                selectedPaths = emptySet()
            )
        }
    }


    /* ---------- 当前目录 ---------- */
    fun setCurrentPath(path: String) {
        _uiState.update {
            it.copy(
                currentPath = path,
                folderContent = null,
                sortedFiles = emptyList(),
                currentIndex = 0,
                previewPath = null,
                selectedPaths = emptySet()
            )
        }
    }

    fun updateCurrentPath(path: String) {
        _uiState.update {
            it.copy(currentPath = path)
        }
    }

    /* ---------- content ---------- */

    enum class ContentUpdateMode {
        REPLACE,   // 切换目录
        REFRESH    // 同目录刷新
    }


    fun updateFolderContent(
        content: FolderContent,
        currentPath: String,
        mode: ContentUpdateMode,
        fileMode: Mode,
        sortMode: String
    ) {
        val files = sortPreviewableFiles(
            files = content.files,
            currentPath = currentPath,
            dateFormat = dateFormat,
            mode = fileMode,
            sortMode = sortMode
        )

        _uiState.update {
            val maxIndex = (files.size - 1).coerceAtLeast(0)

            when (mode) {
                ContentUpdateMode.REPLACE ->
                    it.copy(
                        currentPath = currentPath,
                        folderContent = content,
                        sortedFiles = files,
                        currentIndex = 0,
                        previewPath = null,
                        selectedPaths = emptySet()
                    )

                ContentUpdateMode.REFRESH ->
                    it.copy(
                        currentPath = currentPath,
                        folderContent = content,
                        sortedFiles = files,
                        currentIndex = it.currentIndex.coerceIn(0, maxIndex)
                    )
            }

        }
    }


    /* ---------- preview / selection ---------- */

    fun openPreview(file: FileInfo, index: Int) {
        val key = file.file_url ?: file.path
        _uiState.update {
            it.copy(
                currentIndex = index,
                previewPath = file.net_url,
                selectedPaths = setOf(key)
            )
        }
    }

    fun closePreview() {
        _uiState.update {
            it.copy(previewPath = null)
        }
    }

    fun openPreviewVideo(filePath: String) {
        _uiState.update {
            it.copy(
                previewVideo = filePath
            )
        }
    }

    fun closePreviewVideo() {
        _uiState.update {
            it.copy(previewVideo = null)
        }
    }


    fun selectOnly(file: FileInfo) {
        val key = file.file_url ?: file.path
        _uiState.update {
            it.copy(selectedPaths = setOf(key))
        }
    }

    fun toggleSelect(file: FileInfo) {
        val key = file.file_url ?: file.path
        _uiState.update {
            val next = it.selectedPaths.toMutableSet()
            if (!next.add(key)) next.remove(key)
            it.copy(selectedPaths = next)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedPaths = emptySet())
        }
    }

    fun selectAllFiles(files: List<FileInfo>) {
        val paths = files
            .filter { !it.is_dir }
            .map { it.file_url ?: it.path }
            .toSet()

        _uiState.update {
            it.copy(selectedPaths = paths)
        }
    }



}
