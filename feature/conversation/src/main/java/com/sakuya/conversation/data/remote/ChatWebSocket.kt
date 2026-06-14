package com.sakuya.conversation.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sakuya.data.local.TokenStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

class ChatWebSocket @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenStorage: TokenStorage,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<ChatMessageDto>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<ChatMessageDto> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun connect() {
        val token = tokenStorage.getTokenBlocking()
        val request = Request.Builder()
            .url("wss://api.sakuya.com/ws/chat")
            .header("Authorization", "Bearer $token")
            .build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val payload = gson.fromJson(text, WebSocketPayload::class.java)
                when (payload.type) {
                    "message" -> {
                        val msg = gson.fromJson(payload.data, ChatMessageDto::class.java)
                        _messages.tryEmit(msg)
                    }
                    "typing" -> {}
                    "read" -> {}
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    fun sendMessage(conversationId: String, content: String){
        val payload = gson.toJson(WebSocketPayload(
            type = "message",
            conversationId = conversationId,
            data = gson.toJsonTree(SendMessageRequest(content))
        ))
        webSocket?.send(payload)
    }

    fun disconnect(){
        webSocket?.close(1000,"用户退出")
        webSocket = null
    }
    private fun scheduleReconnect(){

    }
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class WebSocketPayload(
    val type: String,
    val conversationId: String? = null,
    val data: JsonElement? = null
)
