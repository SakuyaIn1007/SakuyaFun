package com.sakuya.backend.chat;

import com.sakuya.backend.common.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ChatMediaService.java
 * 职责说明：保存聊天媒体并在读取前执行会话成员鉴权，不复用公开头像目录。
 * 执行流程：校验成员/类型/大小 -> 使用随机存储名写盘 -> 保存元数据；读取时再次校验成员与规范化路径。
 */
@Service
public class ChatMediaService {
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> FILE_TYPES = Set.of(
        "application/pdf", "text/plain", "application/zip", "application/epub+zip"
    );

    private final ConversationMemberRepository members;
    private final ChatAttachmentRepository attachments;
    private final Path storageRoot;

    public ChatMediaService(ConversationMemberRepository members, ChatAttachmentRepository attachments,
                            @Value("${app.chat-media-dir:${app.upload-dir}/chat}") String storageDir) {
        this.members = members;
        this.attachments = attachments;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** 上传成功后返回未绑定消息的附件；只有同一上传者可以在后续发送时认领。 */
    @Transactional
    public ChatAttachment upload(UUID userId, UUID conversationId, MultipartFile file) throws IOException {
        requireMember(userId, conversationId);
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择附件");
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        String type = contentType.startsWith("image/") ? "image" : "file";
        if (type.equals("image") && !Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType)) {
            throw new BusinessException(400, "仅支持 JPG、PNG、WebP 或 GIF 图片");
        }
        if (type.equals("file") && !FILE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 PDF、TXT、ZIP 或 EPUB 文件");
        }
        long limit = type.equals("image") ? MAX_IMAGE_BYTES : MAX_FILE_BYTES;
        if (file.getSize() > limit) throw new BusinessException(400, type.equals("image") ? "图片不能超过 10MB" : "文件不能超过 20MB");

        Files.createDirectories(storageRoot);
        String extension = extension(contentType);
        String storageName = UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(storageName).normalize();
        if (!target.startsWith(storageRoot)) throw new BusinessException(400, "附件路径无效");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        String originalName = safeName(file.getOriginalFilename());
        try {
            return attachments.save(new ChatAttachment(conversationId, userId, type, contentType, originalName, storageName, file.getSize()));
        } catch (RuntimeException error) {
            Files.deleteIfExists(target);
            throw error;
        }
    }

    /** 下载只能由会话成员完成；数据库存储键必须仍解析在专用目录内。 */
    public MediaContent content(UUID userId, UUID conversationId, UUID attachmentId) throws IOException {
        requireMember(userId, conversationId);
        ChatAttachment attachment = attachments.findByIdAndConversationId(attachmentId, conversationId)
            .orElseThrow(() -> new BusinessException(404, "附件不存在"));
        Path path = storageRoot.resolve(attachment.getStorageName()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) throw new BusinessException(404, "附件文件不存在");
        Resource resource = new UrlResource(path.toUri());
        return new MediaContent(resource, MediaType.parseMediaType(attachment.getContentType()), attachment.getOriginalName(), attachment.getSizeBytes());
    }

    /** 草稿移除时只允许上传者删除尚未绑定消息的附件，已发送媒体不可被该接口破坏。 */
    @Transactional
    public void discard(UUID userId, UUID conversationId, UUID attachmentId) throws IOException {
        requireMember(userId, conversationId);
        ChatAttachment attachment = attachments.findByIdAndConversationId(attachmentId, conversationId)
            .orElseThrow(() -> new BusinessException(404, "附件不存在"));
        if (!attachment.getUploaderId().equals(userId) || attachment.getMessageId() != null) throw new BusinessException(409, "附件已发送或不属于当前用户");
        attachments.delete(attachment);
        Path path = storageRoot.resolve(attachment.getStorageName()).normalize();
        if (path.startsWith(storageRoot)) Files.deleteIfExists(path);
    }

    /** 每小时清理超过一天仍未绑定消息的草稿附件，防止退出页面或进程终止留下永久孤儿文件。 */
    @Scheduled(fixedDelayString = "${app.chat-media-cleanup-delay-ms:3600000}")
    @Transactional
    public void cleanupAbandonedUploads() {
        attachments.findByMessageIdIsNullAndCreatedAtBefore(java.time.Instant.now().minus(java.time.Duration.ofHours(24))).forEach(attachment -> {
            Path path = storageRoot.resolve(attachment.getStorageName()).normalize();
            try {
                if (path.startsWith(storageRoot)) Files.deleteIfExists(path);
                attachments.delete(attachment);
            } catch (IOException ignored) {
                // 文件系统短暂失败时保留元数据，下一轮继续清理，避免数据库记录与文件状态进一步分叉。
            }
        });
    }

    private void requireMember(UUID userId, UUID conversationId) {
        if (members.findByConversationIdAndUserId(conversationId, userId).isEmpty()) throw new BusinessException(403, "你不在该会话中");
    }

    private String safeName(String raw) {
        String name;
        try { name = raw == null ? "attachment" : Path.of(raw).getFileName().toString().trim(); }
        catch (RuntimeException invalidName) { name = "attachment"; }
        return name.isBlank() ? "attachment" : name.substring(0, Math.min(name.length(), 255));
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "application/epub+zip" -> ".epub";
            default -> ".zip";
        };
    }

    public record MediaContent(Resource resource, MediaType contentType, String fileName, long sizeBytes) {}
}
