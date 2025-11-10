package com.kano.mycomfyui.data


data class FileInfo(
    val name: String,
    val is_dir: Boolean,
    val path: String,
    val thumbnail_url: String? = null,
    val file_url: String? = null,
    val date: String? = null,
)

data class FolderContent(
    val parent: FileInfo,
    val folders: List<FileInfo>,
    val files: List<FileInfo>
)

data class RestartResponse(
    val success: Boolean,
    val message: String,
    val new_task_id: String?,
)