package com.sakuya.backend.chat;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ChatService.java
 * 职责说明：协调会话成员权限、单聊幂等创建、消息查询与聊天详情的业务组装。
 * 执行流程：所有操作先确认当前用户是会话成员；随后读写成员级偏好或消息数据；
 * 最后将实体映射为独立 DTO，避免将 JPA 实体和用户隐私字段暴露到 API。
 */
@Service
public class ChatService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final ChatAttachmentRepository attachments;
    private final UserRepository users;
    private final TransactionTemplate transactionTemplate;

    public ChatService(ConversationRepository conversations, ConversationMemberRepository members, ChatMessageRepository messages, ChatAttachmentRepository attachments, UserRepository users, PlatformTransactionManager transactionManager) {
        this.conversations = conversations; this.members = members; this.messages = messages; this.attachments = attachments; this.users = users;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<ConversationDto> conversations(UUID userId) { return members.findByUserId(userId).stream().map(member -> toConversationDto(member, userId)).toList(); }

    @Transactional
    public List<ChatMessageDto> messages(UUID userId, UUID conversationId, Long before, int limit) {
        ConversationMember member = requireMember(userId, conversationId);
        var page = PageRequest.of(0, boundedLimit(limit));
        List<ChatMessage> found = before == null ? messages.findByConversationIdOrderByCreatedAtDesc(conversationId, page) : messages.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(conversationId, Instant.ofEpochMilli(before), page);
        member.markRead();
        return found.reversed().stream().map(message -> toDto(message, userId)).toList();
    }

    /** 仅检索当前会话的文本消息，保证关键词和结果不会跨会话泄露。 */
    public List<ChatMessageDto> searchMessages(UUID userId, UUID conversationId, String keyword, int limit) {
        requireMember(userId, conversationId);
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) return List.of();
        return messages.findByConversationIdAndMessageTypeAndContentContainingIgnoreCaseOrderByCreatedAtDesc(conversationId, "text", normalized, PageRequest.of(0, boundedLimit(limit))).stream().map(message -> toDto(message, userId)).toList();
    }

    /** 为搜索结果回跳读取目标消息前后记录，客户端写入 Room 后滚动到目标消息。 */
    public List<ChatMessageDto> messageContext(UUID userId, UUID conversationId, UUID messageId, int around) {
        requireMember(userId, conversationId);
        ChatMessage target = messages.findById(messageId).filter(message -> message.getConversationId().equals(conversationId)).orElseThrow(() -> new BusinessException(404, "消息不存在"));
        int count = Math.max(1, Math.min(around, 50));
        List<ChatMessage> context = new ArrayList<>(messages.findByConversationIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(conversationId, target.getCreatedAt(), PageRequest.of(0, count)));
        context.addAll(messages.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(conversationId, target.getCreatedAt(), PageRequest.of(0, count)));
        return context.stream().sorted(Comparator.comparing(ChatMessage::getCreatedAt)).map(message -> toDto(message, userId)).toList();
    }

    @Transactional
    public ChatMessage save(UUID userId, UUID conversationId, String content, String type) {
        return save(userId, conversationId, content, type, List.of(), null, null);
    }

    @Transactional
    public ChatMessage save(UUID userId, UUID conversationId, String content, String type, List<UUID> attachmentIds, UUID replyToMessageId) {
        return save(userId, conversationId, content, type, attachmentIds, replyToMessageId, null);
    }

    /**
     * 保存带附件或回复的消息。
     * 执行流程：校验会话成员与回复归属 -> 校验未绑定附件均由发送者上传 -> 保存消息 -> 原子绑定附件。
     */
    @Transactional
    public ChatMessage save(UUID userId, UUID conversationId, String content, String type, List<UUID> attachmentIds, UUID replyToMessageId, String clientMessageId) {
        requireMember(userId, conversationId);
        String normalizedClientId = clientMessageId == null ? null : clientMessageId.trim();
        if (normalizedClientId != null && !normalizedClientId.isBlank()) {
            if (normalizedClientId.length() > 100) throw new BusinessException(400, "客户端消息 ID 过长");
            ChatMessage existing = messages.findBySenderIdAndClientMessageId(userId, normalizedClientId).orElse(null);
            if (existing != null) {
                if (!existing.getConversationId().equals(conversationId)) throw new BusinessException(409, "客户端消息 ID 已用于其他会话");
                return existing;
            }
        }
        String normalizedContent = content == null ? "" : content.trim();
        List<UUID> normalizedIds = attachmentIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(attachmentIds));
        if (normalizedIds.size() > 3) throw new BusinessException(400, "每条消息最多发送 3 个附件");
        if (normalizedContent.isBlank() && normalizedIds.isEmpty()) throw new BusinessException(400, "消息内容和附件不能同时为空");
        if (normalizedContent.length() > 4000) throw new BusinessException(400, "消息内容过长");

        List<ChatAttachment> claimed = attachments.findAllById(normalizedIds);
        if (claimed.size() != normalizedIds.size() || claimed.stream().anyMatch(attachment ->
            !attachment.getConversationId().equals(conversationId) || !attachment.getUploaderId().equals(userId) || attachment.getMessageId() != null)) {
            throw new BusinessException(400, "附件不存在、已使用或不属于当前会话");
        }
        ChatMessage replied = replyToMessageId == null ? null : messages.findById(replyToMessageId)
            .filter(message -> message.getConversationId().equals(conversationId))
            .orElseThrow(() -> new BusinessException(400, "被回复的消息不存在"));
        String replySender = replied == null ? null : users.findById(replied.getSenderId()).map(User::getNickname).orElse("未知用户");
        String replyPreview = replied == null ? null : preview(replied);
        if (type != null && !type.isBlank() && !Set.of("text", "image", "file").contains(type)) throw new BusinessException(400, "不支持的消息类型");
        // 无附件时保留早期客户端的显式类型兼容；新媒体消息始终由已认领附件推导可信类型。
        String resolvedType = normalizedIds.isEmpty() ? (type == null || type.isBlank() ? "text" : type) : claimed.stream().allMatch(item -> item.getType().equals("image")) ? "image" : "file";
        ChatMessage saved = messages.save(new ChatMessage(conversationId, userId, normalizedContent, resolvedType, replyToMessageId, replySender, replyPreview, normalizedClientId));
        claimed.forEach(attachment -> attachment.bindToMessage(saved.getId()));
        attachments.saveAll(claimed);
        return saved;
    }

    public ConversationDetailDto detail(UUID userId, UUID conversationId) {
        ConversationMember current = requireMember(userId, conversationId);
        Conversation conversation = requireConversation(conversationId);
        List<ConversationMember> all = members.findByConversationId(conversationId);
        DirectProfileDto direct = conversation.getType() == Conversation.Type.DIRECT ? all.stream().filter(member -> !member.getUserId().equals(userId)).findFirst().map(this::toDirectProfile).orElse(null) : null;
        List<MemberDto> groupMembers = conversation.getType() == Conversation.Type.GROUP ? all.stream().map(this::toMemberDto).toList() : List.of();
        // 单聊实体标题只保存创建时快照；详情必须按当前查看者使用对方昵称，避免接收方看到自己的名字。
        String displayTitle = direct == null ? conversation.getTitle() : direct.nickname();
        return new ConversationDetailDto(conversationId.toString(), conversation.getType().name().toLowerCase(java.util.Locale.ROOT), displayTitle, avatar(displayTitle), conversation.getDescription(), conversation.getAnnouncement(), direct, groupMembers, new PreferencesDto(current.isPinned(), current.isMuted(), current.getMemberNickname()));
    }

    @Transactional
    public ConversationDetailDto updatePreferences(UUID userId, UUID conversationId, Boolean pinned, Boolean muted, String memberNickname) {
        ConversationMember member = requireMember(userId, conversationId);
        Conversation conversation = requireConversation(conversationId);
        if (memberNickname != null && conversation.getType() != Conversation.Type.GROUP) throw new BusinessException(400, "只有群聊可以设置群名片");
        if (memberNickname != null && memberNickname.trim().length() > 40) throw new BusinessException(400, "群名片不能超过 40 个字符");
        member.updatePreferences(pinned, muted, memberNickname);
        return detail(userId, conversationId);
    }

    @Transactional
    public void leaveGroup(UUID userId, UUID conversationId) {
        if (requireConversation(conversationId).getType() != Conversation.Type.GROUP) throw new BusinessException(400, "单聊不支持退出会话");
        requireMember(userId, conversationId);
        members.deleteByConversationIdAndUserId(conversationId, userId);
    }

    public ChatMessageDto toDto(ChatMessage message, UUID viewer) {
        User sender = users.findById(message.getSenderId()).orElse(null);
        String name = sender == null ? "" : sender.getNickname();
        List<ChatAttachmentDto> media = attachments.findByMessageIdOrderByCreatedAtAsc(message.getId()).stream().map(this::attachmentDto).toList();
        ChatMessageReplyDto reply = message.getReplyToMessageId() == null ? null : new ChatMessageReplyDto(
            message.getReplyToMessageId().toString(), message.getReplySenderName(), message.getReplyPreview()
        );
        return new ChatMessageDto(message.getId().toString(), message.getConversationId().toString(), message.getContent(), label(message.getCreatedAt()), message.getSenderId().equals(viewer), avatar(name), message.getCreatedAt().toEpochMilli(), message.getMessageType(), message.getSenderId().toString(), name, media, reply, "unread");
    }
    public List<UUID> memberIds(UUID conversationId) { return members.findByConversationId(conversationId).stream().map(ConversationMember::getUserId).toList(); }
    @Transactional public Conversation create(String title, List<UUID> userIds) { if (userIds.size() < 2) throw new BusinessException(400, "会话至少需要两名成员"); Conversation conversation = conversations.save(new Conversation(title, Conversation.Type.GROUP)); userIds.stream().distinct().forEach(id -> members.save(new ConversationMember(conversation.getId(), id))); return conversation; }
    /**
     * 创建或复用双方直聊。
     * 执行流程：规范化双方 ID -> 在独立事务中按唯一键/旧成员关系查找 -> 必要时创建；
     * 并发插入若触发唯一约束，则在新事务读取胜出的会话返回，调用方不会得到重复会话。
     */
    public ConversationDto direct(UUID userId, UUID targetId) {
        if (userId.equals(targetId)) throw new BusinessException(400, "不能和自己创建单聊");
        String directKey = directKey(userId, targetId);
        try {
            return transactionTemplate.execute(status -> createOrReuseDirect(userId, targetId, directKey));
        } catch (DataIntegrityViolationException conflict) {
            Conversation winner = transactionTemplate.execute(status -> conversations.findByDirectKey(directKey).orElseThrow(() -> conflict));
            return toConversationDto(requireMember(userId, winner.getId()), userId);
        }
    }
    @Transactional public void markRead(UUID userId, UUID conversationId) { requireMember(userId, conversationId).markRead(); }
    private ConversationDto toConversationDto(ConversationMember member, UUID viewer) {
        Conversation conversation = requireConversation(member.getConversationId()); List<ConversationMember> all = members.findByConversationId(conversation.getId());
        String title = conversation.getType() == Conversation.Type.DIRECT ? all.stream().map(ConversationMember::getUserId).filter(id -> !id.equals(viewer)).findFirst().flatMap(users::findById).map(User::getNickname).orElse(conversation.getTitle()) : conversation.getTitle();
        Optional<ChatMessage> last = messages.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId()); long unread = messages.countByConversationIdAndCreatedAtAfterAndSenderIdNot(conversation.getId(), Instant.ofEpochMilli(member.getLastReadAt()), viewer);
        return new ConversationDto(conversation.getId().toString(), title, last.map(ChatMessage::getContent).orElse(""), last.map(message -> label(message.getCreatedAt())).orElse(""), (int) Math.min(unread, Integer.MAX_VALUE), avatar(title), member.isPinned());
    }
    private DirectProfileDto toDirectProfile(ConversationMember member) { User user = users.findById(member.getUserId()).orElseThrow(() -> new BusinessException(404, "用户不存在")); return new DirectProfileDto(user.getId().toString(), user.getNickname(), user.getAvatarUrl(), user.getSignature(), false); }
    private MemberDto toMemberDto(ConversationMember member) { User user = users.findById(member.getUserId()).orElseThrow(() -> new BusinessException(404, "用户不存在")); String name = member.getMemberNickname().isBlank() ? user.getNickname() : member.getMemberNickname(); return new MemberDto(user.getId().toString(), name, avatar(user.getNickname())); }
    private Conversation requireConversation(UUID id) { return conversations.findById(id).orElseThrow(() -> new BusinessException(404, "会话不存在")); }
    private ConversationMember requireMember(UUID user, UUID conversation) { return members.findByConversationIdAndUserId(conversation, user).orElseThrow(() -> new BusinessException(403, "你不在该会话中")); }
    private ConversationDto createOrReuseDirect(UUID userId, UUID targetId, String directKey) {
        User target = users.findById(targetId).orElseThrow(() -> new BusinessException(404, "用户不存在"));
        Conversation keyed = conversations.findByDirectKey(directKey).orElse(null);
        if (keyed != null) return toConversationDto(requireMember(userId, keyed.getId()), userId);

        // 升级前的单聊没有 directKey；只复用恰好包含双方的二人单聊，避免误命中异常成员数据。
        for (ConversationMember membership : members.findByUserId(userId)) {
            Conversation candidate = conversations.findById(membership.getConversationId()).orElse(null);
            if (candidate == null || candidate.getType() != Conversation.Type.DIRECT) continue;
            List<ConversationMember> all = members.findByConversationId(candidate.getId());
            if (all.size() == 2 && all.stream().anyMatch(item -> item.getUserId().equals(targetId))) {
                candidate.assignDirectKey(directKey);
                conversations.saveAndFlush(candidate);
                return toConversationDto(membership, userId);
            }
        }

        Conversation created = conversations.saveAndFlush(new Conversation(target.getNickname(), directKey));
        members.save(new ConversationMember(created.getId(), userId));
        members.save(new ConversationMember(created.getId(), targetId));
        return toConversationDto(requireMember(userId, created.getId()), userId);
    }
    private static String directKey(UUID first, UUID second) {
        String left = first.toString(); String right = second.toString();
        return left.compareTo(right) <= 0 ? left + ":" + right : right + ":" + left;
    }
    private static int boundedLimit(int value) { return Math.max(1, Math.min(value, 100)); }
    private ChatAttachmentDto attachmentDto(ChatAttachment attachment) {
        String url = "/conversations/" + attachment.getConversationId() + "/attachments/" + attachment.getId() + "/content";
        return new ChatAttachmentDto(attachment.getId().toString(), attachment.getType(), url, attachment.getOriginalName(), attachment.getSizeBytes(), attachment.getType().equals("image") ? url : null);
    }
    private String preview(ChatMessage message) {
        String value = message.getContent() == null ? "" : message.getContent().trim();
        if (value.isBlank()) value = message.getMessageType().equals("image") ? "[图片]" : "[文件]";
        return value.substring(0, Math.min(value.length(), 120));
    }
    private static String label(Instant time) { return DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(time); }
    private static String avatar(String text) { return text == null || text.isBlank() ? "?" : text.substring(0, 1); }
    public record ConversationDto(String id, String title, String lastMessage, String timeLabel, int unreadCount, String avatarText, boolean isPinned) {}
    public record ChatMessageDto(String id, String conversationId, String content, String timeLabel, boolean isMine, String avatarText, long timeStamp, String messageType, String senderId, String senderName, List<ChatAttachmentDto> attachments, ChatMessageReplyDto replyTo, String readStatus) {}
    public record ChatAttachmentDto(String id, String type, String url, String name, Long sizeBytes, String thumbnailUrl) {}
    public record ChatMessageReplyDto(String messageId, String senderName, String preview) {}
    public record DirectProfileDto(String userId, String nickname, String avatarUrl, String signature, boolean isOnline) {}
    public record MemberDto(String userId, String displayName, String avatarText) {}
    public record PreferencesDto(boolean pinned, boolean muted, String memberNickname) {}
    public record ConversationDetailDto(String id, String type, String title, String avatarText, String description, String announcement, DirectProfileDto directProfile, List<MemberDto> members, PreferencesDto preferences) {}
}
