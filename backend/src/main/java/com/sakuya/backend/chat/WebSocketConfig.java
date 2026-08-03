package com.sakuya.backend.chat;
import org.springframework.context.annotation.Configuration; import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
 private final ChatWebSocketHandler handler; private final ChatHandshakeInterceptor auth;
 public WebSocketConfig(ChatWebSocketHandler handler,ChatHandshakeInterceptor auth){this.handler=handler;this.auth=auth;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/ws/chat").addInterceptors(auth).setAllowedOriginPatterns("*");}
}
