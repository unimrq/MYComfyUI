package com.kano.mycomfyui.network

import com.kano.mycomfyui.data.FolderContent
import com.kano.mycomfyui.data.RestartResponse
import com.kano.mycomfyui.ui.TaskInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    data class ClearTaskResponse(
        val status: String,
        val message: String
    )

    @GET("/api/tasks/clear")
    suspend fun clearTasks(): ClearTaskResponse

    @POST("/api/tasks/{id}/restart")
    suspend fun restartTask(@Path("id") id: String): RestartResponse

    /** 扫描文件夹（后台执行） */
    @GET("/api/scan")
    suspend fun scanFolder(): ResponseBody

    /** 检测文件夹（后台执行） */
    @GET("/api/detect")
    suspend fun detectFolder(): ResponseBody

    /** 数据模型：前端使用的词条对象 */
    data class PromptItem(
        val title: String = "",
        val text: String = ""
    )

    /** 获取快捷词条列表，返回 title + text */
    @GET("/api/prompts")
    suspend fun getPromptList(): List<PromptItem>

    /** 新增快捷词条 */
    @POST("/api/prompts")
    @FormUrlEncoded
    suspend fun addPrompt(
        @Field("title") title: String,
        @Field("text") text: String
    ): Response<Unit>

    /** 修改快捷词条 */
    @PUT("/api/prompts")
    @FormUrlEncoded
    suspend fun updatePrompt(
        @Field("oldTitle") oldTitle: String,
        @Field("newTitle") newTitle: String,
        @Field("newText") newText: String
    ): Response<Unit>

    /** 删除快捷词条 */
    @DELETE("/api/prompts")
    suspend fun deletePrompt(
        @Query("title") title: String
    ): Response<Unit>

    @POST("/api/folder/create")
    @FormUrlEncoded
    suspend fun createFolder(
        @Field("parent") parent: String,
        @Field("name") name: String
    ): Response<Unit>

    @FormUrlEncoded
    @POST("/api/file/move")
    suspend fun moveFile(
        @Field("src") src: String,
        @Field("dest") dest: String
    ): Response<Unit>

    data class ServerStatusResponse(
        val alive: Boolean
    )

    /** 获取服务器运行状态 */
    @GET("/api/server/status")
    suspend fun getServerStatus(): ServerStatusResponse

}
