package com.kano.mycomfyui.network

import okhttp3.*
import okio.ByteString

class LogWebSocket(
    private val url: String,
    private val onMessage: (String) -> Unit
) {

    private val client = OkHttpClient()
    private var ws: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(url).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onMessage("✅ WebSocket 已连接")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onMessage("❌ WebSocket 连接失败: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onMessage("⚠️ WebSocket 正在关闭: $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onMessage("ℹ️ WebSocket 已关闭: $reason")
            }
        })
    }

    fun close() {
        ws?.close(1000, "App退出")
    }

    fun send(message: String) {
        ws?.send(message)
    }
}
