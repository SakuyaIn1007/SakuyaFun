package com.sakuya.conversation.data.remote

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.sakuya.data.BuildConfig
import com.sakuya.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

class ChatWebSocket @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionManager: SessionManager,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectAttempts = 0
    private var manuallyDisconnected = false
    private val _messages = MutableSharedFlow<ChatMessageDto>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<ChatMessageDto> = _messages

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            return
        }
        manuallyDisconnected = false
        _connectionState.value = if (reconnectAttempts > 0) {
            ConnectionState.RECONNECTING
        } else {
            ConnectionState.CONNECTING
        }
        val token = sessionManager.getTokenBlocking()
        val requestBuilder = Request.Builder()
            .url(BuildConfig.WS_CHAT_URL)

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
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
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@ChatWebSocket.webSocket = null
                _connectionState.value = ConnectionState.FAILED
                if (response?.code == 401) {
                    manuallyDisconnected = true
                    scope.launch { sessionManager.expireSession() }
                    return
                }
                if (!manuallyDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@ChatWebSocket.webSocket = null
                _connectionState.value = ConnectionState.DISCONNECTED
                if (!manuallyDisconnected) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun sendMessage(conversationId: String, content: String): Boolean {
        val payload = gson.toJson(WebSocketPayload(
            type = "message",
            conversationId = conversationId,
            data = gson.toJsonTree(SendMessageRequest(content))
        ))
        return webSocket?.send(payload) == true
    }

    fun disconnect(){
        manuallyDisconnected = true
        reconnectAttempts = 0
        webSocket?.close(1000,"用户退出")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    private fun scheduleReconnect(){
        reconnectAttempts += 1
        val delayMillis = (1_000L * (1 shl (reconnectAttempts - 1))).coerceAtMost(30_000L)
        scope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            delay(delayMillis)
            if (!manuallyDisconnected) {
                connect()
            }
        }
    }
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED
}

data class WebSocketPayload(
    val type: String,
    val conversationId: String? = null,
    val data: JsonElement? = null
)
