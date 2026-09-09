package com.sakuya.backend.chat;
import com.sakuya.backend.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/**
 * ConversationController.java
 * 职责说明：提供会话、聊天详情和历史检索的 HTTP 边界。
 * 执行流程：控制器仅解析参数和校验请求体，成员鉴权、状态更新和 DTO 组装全部交给 ChatService。
 */
@RestController @RequestMapping("/conversations")
public class ConversationController {
 private final ChatService chat; private final ChatMediaService media;
 public ConversationController(ChatService chat,ChatMediaService media){this.chat=chat;this.media=media;}
 @GetMapping ApiResponse<List<ChatService.ConversationDto>> list(Authentication auth){return ApiResponse.ok(chat.conversations(AuthSupport.userId(auth)));}
 @GetMapping("/{id}") ApiResponse<ChatService.ConversationDetailDto> detail(Authentication auth,@PathVariable UUID id){return ApiResponse.ok(chat.detail(AuthSupport.userId(auth),id));}
 @GetMapping("/{id}/messages") ApiResponse<List<ChatService.ChatMessageDto>> messages(Authentication auth,@PathVariable UUID id,@RequestParam(required=false)Long before,@RequestParam(defaultValue="30")int limit){return ApiResponse.ok(chat.messages(AuthSupport.userId(auth),id,before,limit));}
 @GetMapping("/{id}/messages/search") ApiResponse<List<ChatService.ChatMessageDto>> search(Authentication auth,@PathVariable UUID id,@RequestParam String keyword,@RequestParam(defaultValue="30")int limit){return ApiResponse.ok(chat.searchMessages(AuthSupport.userId(auth),id,keyword,limit));}
 @GetMapping("/{id}/messages/context") ApiResponse<List<ChatService.ChatMessageDto>> context(Authentication auth,@PathVariable UUID id,@RequestParam UUID messageId,@RequestParam(defaultValue="20")int around){return ApiResponse.ok(chat.messageContext(AuthSupport.userId(auth),id,messageId,around));}
 @PostMapping("/{id}/messages") ApiResponse<ChatService.ChatMessageDto> send(Authentication auth,@PathVariable UUID id,@Valid@RequestBody SendMessageRequest r){UUID me=AuthSupport.userId(auth);return ApiResponse.ok(chat.toDto(chat.save(me,id,r.content(),r.messageType(),r.attachmentIds(),r.replyToMessageId(),r.clientMessageId()),me));}
 /** 先上传媒体取得附件 ID，再发送消息；上传本身不会向其他成员广播。 */
 @PostMapping(value="/{id}/attachments",consumes="multipart/form-data") ApiResponse<ChatService.ChatAttachmentDto> upload(Authentication auth,@PathVariable UUID id,@RequestPart("file")MultipartFile file)throws IOException{ChatAttachment attachment=media.upload(AuthSupport.userId(auth),id,file);String url="/conversations/"+id+"/attachments/"+attachment.getId()+"/content";return ApiResponse.ok(new ChatService.ChatAttachmentDto(attachment.getId().toString(),attachment.getType(),url,attachment.getOriginalName(),attachment.getSizeBytes(),attachment.getType().equals("image")?url:null));}
 /** 内容读取仍要求 JWT 与会话成员身份，避免知道 URL 即可访问聊天文件。 */
 @GetMapping("/{id}/attachments/{attachmentId}/content") ResponseEntity<Resource> attachment(Authentication auth,@PathVariable UUID id,@PathVariable UUID attachmentId)throws IOException{ChatMediaService.MediaContent content=media.content(AuthSupport.userId(auth),id,attachmentId);return ResponseEntity.ok().contentType(content.contentType()).contentLength(content.sizeBytes()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(content.fileName(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(content.resource());}
 @DeleteMapping("/{id}/attachments/{attachmentId}") ApiResponse<Void> discardAttachment(Authentication auth,@PathVariable UUID id,@PathVariable UUID attachmentId)throws IOException{media.discard(AuthSupport.userId(auth),id,attachmentId);return ApiResponse.ok();}
 @PutMapping("/{id}/read") ApiResponse<Void> read(Authentication auth,@PathVariable UUID id){chat.markRead(AuthSupport.userId(auth),id);return ApiResponse.ok();}
 @PatchMapping("/{id}/preferences") ApiResponse<ChatService.ConversationDetailDto> preferences(Authentication auth,@PathVariable UUID id,@Valid@RequestBody UpdatePreferencesRequest r){return ApiResponse.ok(chat.updatePreferences(AuthSupport.userId(auth),id,r.pinned(),r.muted(),r.memberNickname()));}
 @DeleteMapping("/{id}/membership") ApiResponse<Void> leave(Authentication auth,@PathVariable UUID id){chat.leaveGroup(AuthSupport.userId(auth),id);return ApiResponse.ok();}
 @PostMapping ApiResponse<Map<String,String>> create(Authentication auth,@Valid@RequestBody CreateConversationRequest r){List<UUID> ids=new ArrayList<>(r.userIds());ids.add(AuthSupport.userId(auth));Conversation c=chat.create(r.title(),ids);return ApiResponse.ok(Map.of("id",c.getId().toString()));}
 public record SendMessageRequest(@Size(max=4000)String content,@Pattern(regexp="text|image|file")String messageType,@Size(max=3)List<UUID> attachmentIds,UUID replyToMessageId,@Size(max=100)String clientMessageId){public SendMessageRequest{attachmentIds=attachmentIds==null?List.of():List.copyOf(attachmentIds);}}
 public record UpdatePreferencesRequest(Boolean pinned,Boolean muted,@Size(max=40)String memberNickname){}
 public record CreateConversationRequest(@NotBlank@Size(max=100)String title,@NotEmpty List<UUID> userIds){}
}
