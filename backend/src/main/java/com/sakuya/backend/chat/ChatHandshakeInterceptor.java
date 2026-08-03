package com.sakuya.backend.chat;
import com.sakuya.backend.security.JwtService; import com.sakuya.backend.user.UserRepository; import java.util.Map; import org.springframework.http.HttpStatus; import org.springframework.http.server.*; import org.springframework.stereotype.Component; import org.springframework.web.socket.*; import org.springframework.web.socket.server.HandshakeInterceptor;
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
 private final JwtService jwt; private final UserRepository users; public ChatHandshakeInterceptor(JwtService jwt,UserRepository users){this.jwt=jwt;this.users=users;}
 @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes){String h=request.getHeaders().getFirst("Authorization");if(h==null||!h.startsWith("Bearer ")){response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;}try{var userId=jwt.parseUserId(h.substring(7));if(!users.existsById(userId)){response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;}attributes.put("userId",userId);return true;}catch(Exception e){response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;}}
 @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception){}
}
