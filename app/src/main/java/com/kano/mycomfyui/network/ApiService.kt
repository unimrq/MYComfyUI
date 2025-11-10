package com.kano.mycomfyui.network

import com.kano.mycomfyui.data.FolderContent
import com.kano.mycomfyui.data.RestartResponse
import com.kano.mycomfyui.ui.TaskInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("/api/browse")
    suspend fun browse(
        @Query("path") path: String = ""
    ): FolderContent

    @GET("/api/gif_templates")
    suspend fun getGifTemplates(): List<String>

    @POST("/api/generate/{type}")
    suspend fun generateImage(
        @Path("type") type: String,
        @Query("imageUrl") imageUrl: String,
        @Query("thumbnailUrl") thumbnailUrl: String = "",
        @Body args: Map<String, String> = emptyMap()

    ): ResponseBody

    data class UploadResponse(
        val success: Boolean,
        val file: String?
    )

    /** 新增：上传图片 */
    @Multipart
    @POST("/api/upload")
    suspend fun uploadImage(
        @Part("path") path: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>


    /** 刷新目录 */
    @POST("/api/refresh")
    suspend fun refresh(
        @Query("path") folder: String
    ): ResponseBody

    @DELETE("/api/delete_file")
    suspend fun deleteFile(
        @Query("path") path: String
    ): ResponseBody

    @GET("/api/tasks")
    suspend fun getTasks(): List<TaskInfo>

    @POST("/api/tasks/{id}/restart")
    suspend fun restartTask(@Path("id") id: String): RestartResponse



}
