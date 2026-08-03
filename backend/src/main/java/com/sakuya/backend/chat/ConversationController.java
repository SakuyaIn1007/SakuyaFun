package com.sakuya.backend.chat;
import com.sakuya.backend.common.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/conversations")
public class ConversationController {
 private final ChatService chat; public ConversationController(ChatService chat){this.chat=chat;}
 @GetMapping ApiResponse<List<ChatService.ConversationDto>> list(Authentication auth){return ApiResponse.ok(chat.conversations(AuthSupport.userId(auth)));}
 @GetMapping("/{id}/messages") ApiResponse<List<ChatService.ChatMessageDto>> messages(Authentication auth,@PathVariable UUID id,@RequestParam(required=false)Long before,@RequestParam(defaultValue="30")int limit){return ApiResponse.ok(chat.messages(AuthSupport.userId(auth),id,before,limit));}
 @PostMapping("/{id}/messages") ApiResponse<ChatService.ChatMessageDto> send(Authentication auth,@PathVariable UUID id,@Valid @RequestBody SendMessageRequest r){UUID me=AuthSupport.userId(auth);return ApiResponse.ok(chat.toDto(chat.save(me,id,r.content(),r.messageType()),me));}
 @PutMapping("/{id}/read") ApiResponse<Void> read(Authentication auth,@PathVariable UUID id){chat.markRead(AuthSupport.userId(auth),id);return ApiResponse.ok();}
 @PostMapping ApiResponse<Map<String,String>> create(Authentication auth,@Valid @RequestBody CreateConversationRequest r){List<UUID> ids=new ArrayList<>(r.userIds());ids.add(AuthSupport.userId(auth));Conversation c=chat.create(r.title(),ids);return ApiResponse.ok(Map.of("id",c.getId().toString()));}
 public record SendMessageRequest(@NotBlank @Size(max=4000)String content,@Pattern(regexp="text|image|file") String messageType){} public record CreateConversationRequest(@NotBlank @Size(max=100) String title,@NotEmpty List<UUID> userIds){}
}
