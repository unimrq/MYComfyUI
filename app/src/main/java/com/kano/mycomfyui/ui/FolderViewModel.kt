package com.kano.mycomfyui.ui

import androidx.lifecycle.ViewModel
import com.kano.mycomfyui.data.FileInfo
import com.kano.mycomfyui.data.FolderContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale


fun sortPreviewableFiles(
    files: List<FileInfo>,
    currentPath: String,
    dateFormat: SimpleDateFormat
): List<FileInfo> {

    fun FileInfo.isPreviewable(): Boolean =
        file_url?.matches(
            Regex(".*\\.(png|jpg|jpeg|gif|mp4|bmp)$", RegexOption.IGNORE_CASE)
        ) == true

    fun getCoreName(name: String): String {
        var base = name.substringBeforeLast(".")
        base = base.substringBefore("-换衣")
        base = base.replace(Regex("[ab]$"), "")
        return base
    }

    val validFiles = files
        .filter { !it.is_dir }
        .filter { it.isPreviewable() }

    val isMp4Only =
        validFiles.isNotEmpty() &&
                validFiles.all { it.file_url?.endsWith(".mp4", true) == true }

    return if (isMp4Only || currentPath == "修图") {
        validFiles.sortedByDescending {
            runCatching {
                dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE
            }.getOrDefault(Long.MAX_VALUE)
        }
    } else {
        validFiles
            .groupBy { getCoreName(it.name) }
            .toList()
            .sortedByDescending { (_, group) ->
                group.minOfOrNull {
                    runCatching {
                        dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE
                    }.getOrDefault(Long.MAX_VALUE)
                } ?: Long.MAX_VALUE
            }
            .flatMap { (_, group) ->
                group.sortedWith(
                    compareBy<FileInfo> {
                        runCatching {
                            dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE
                        }.getOrDefault(Long.MAX_VALUE)
                    }.thenBy { it.name.lowercase() }
                )
            }
    }
}

data class ImageViewerState(
    val currentPath: String = "",
    val folderContent: FolderContent? = null,
    val sortedFiles: List<FileInfo> = emptyList(),
    val currentIndex: Int = 0,
    val previewPath: String? = null,
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

    fun enterDirectory(
        path: String,
        content: FolderContent
    ) {
        val files = sortPreviewableFiles(
            files = content.files,
            currentPath = path,
            dateFormat = dateFormat
        )

        _uiState.update {
            it.copy(
                currentPath = path,
                folderContent = content,
                sortedFiles = files,
                currentIndex = 0,
                previewPath = null,
                selectedPaths = emptySet()
            )
        }
    }

    fun refreshCurrentDirectory(content: FolderContent) {
        val path = _uiState.value.currentPath
        val files = sortPreviewableFiles(
            files = content.files,
            currentPath = path,
            dateFormat = dateFormat
        )

        _uiState.update {
            val maxIndex = (files.size - 1).coerceAtLeast(0)
            it.copy(
                folderContent = content,
                sortedFiles = files,
                currentIndex = it.currentIndex.coerceIn(0, maxIndex)
            )
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
        mode: ContentUpdateMode
    ) {
        val files = sortPreviewableFiles(
            files = content.files,
            currentPath = currentPath,
            dateFormat = dateFormat
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


    fun removeFilesByPath(paths: Set<String>) {
        _uiState.update {
            val newFiles = it.sortedFiles.filterNot { f ->
                f.file_url in paths || f.path in paths
            }

            val maxIndex = (newFiles.size - 1).coerceAtLeast(0)
            it.copy(
                sortedFiles = newFiles,
                currentIndex = it.currentIndex.coerceIn(0, maxIndex)
            )
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

    fun updateCurrentIndex(index: Int) {
        _uiState.update {
            it.copy(currentIndex = index)
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
