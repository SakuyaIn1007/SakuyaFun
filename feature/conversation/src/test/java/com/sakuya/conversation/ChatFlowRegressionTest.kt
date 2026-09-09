package com.sakuya.conversation

import com.google.gson.Gson
import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.data.remote.ChatAttachmentDto
import com.sakuya.conversation.data.remote.ChatMessageReplyDto
import com.sakuya.conversation.data.repository.removeLeftConversationFromRoom
import com.sakuya.conversation.data.repository.toDomain
import com.sakuya.conversation.data.repository.toEntity
import com.sakuya.model.chat.ChatAttachmentType
import com.sakuya.conversation.data.repository.writeConversationPreferencesToRoom
import com.sakuya.conversation.data.repository.writeMessageContextToRoom
import com.sakuya.conversation.data.repository.writeHistoryMessagesToRoom
import com.sakuya.model.chat.ChatMessage
import com.sakuya.model.chat.ConversationDetail
import com.sakuya.model.chat.ConversationPreferences
import com.sakuya.model.chat.ConversationType
import com.sakuya.conversation.ui.findFocusMessageIndex
import com.sakuya.conversation.ui.isFocusMessage
import com.sakuya.data.local.entity.ConversationEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChatFlowRegressionTest.kt
 * 职责说明：覆盖聊天历史搜索结果映射、回跳上下文写入 Room 前的批量转换，以及定位高亮规则。
 * 执行流程：构造服务端 DTO -> 验证领域映射 -> 记录父会话与消息写入顺序 ->
 * 使用写入后的消息列表校验目标索引和唯一高亮，避免依赖设备或真实网络。
 */
class ChatFlowRegressionTest {
    @Test
    fun keywordSearchResultMapsToDomainMessage() {
        val result = dto(id = "matched", content = "包含 Kotlin 关键词", timestamp = 20L).toDomain()

        assertEquals("matched", result.id)
        assertEquals("包含 Kotlin 关键词", result.content)
        assertEquals("sender-1", result.senderId)
        assertEquals(20L, result.timestamp)
    }

    @Test
    fun attachmentAndReplySurviveDtoAndRoomMapping() {
        val gson = Gson()
        val domain = ChatMessageDto(
            id = "media-message", conversationId = "conversation-1", senderId = "sender-1",
            content = "附带说明", timeLabel = "10:00", isMine = true, timeStamp = 30L, messageType = "image",
            attachments = listOf(ChatAttachmentDto("attachment-1", "image", "https://example.test/media/1", "proof.png", 128L, "https://example.test/media/1")),
            replyTo = ChatMessageReplyDto("origin-1", "咲夜", "原消息预览"),
        ).toDomain()

        val restored = domain.toEntity(gson).toDomain(gson)

        assertEquals(ChatAttachmentType.IMAGE, restored.attachments.single().type)
        assertEquals("https://example.test/media/1", restored.attachments.single().url)
        assertEquals("proof.png", restored.attachments.single().name)
        assertEquals("origin-1", restored.replyTo?.messageId)
        assertEquals("原消息预览", restored.replyTo?.preview)
    }

    @Test
    fun contextEnsuresParentThenWritesMessagesForRoom() = runTest {
        val calls = mutableListOf<String>()
        var writtenIds = emptyList<String>()

        writeMessageContextToRoom(
            conversationId = "conversation-1",
            items = listOf(dto("before", "前文", 10L), dto("target", "目标", 20L), dto("after", "后文", 30L)),
            gson = Gson(),
            ensureConversation = { id, title ->
                assertEquals("conversation-1", id)
                assertEquals("", title)
                calls += "conversation"
            },
            writeMessages = { messages ->
                calls += "messages"
                writtenIds = messages.map { it.id }
                assertTrue(messages.all { it.conversationId == "conversation-1" })
            },
        )

        assertEquals(listOf("conversation", "messages"), calls)
        assertEquals(listOf("before", "target", "after"), writtenIds)
    }

    @Test
    fun firstOpenHistoryEnsuresParentWritesMessagesThenMarksRead() = runTest {
        val calls = mutableListOf<String>()
        var writtenIds = emptyList<String>()

        writeHistoryMessagesToRoom(
            conversationId = "conversation-1",
            items = listOf(dto("first", "首次打开可见", 10L)),
            gson = Gson(),
            ensureConversation = { id, title ->
                assertEquals("conversation-1", id)
                assertEquals("", title)
                calls += "conversation"
            },
            writeMessages = { messages ->
                calls += "messages"
                writtenIds = messages.map { it.id }
            },
            markRead = { id ->
                assertEquals("conversation-1", id)
                calls += "read"
            },
        )

        assertEquals(listOf("conversation", "messages", "read"), calls)
        assertEquals(listOf("first"), writtenIds)
    }

    @Test
    fun jumpTargetUsesExactIndexAndUniqueHighlight() {
        val messages = listOf(
            domain("before", 10L),
            domain("target", 20L),
            domain("after", 30L),
        )

        assertEquals(1, findFocusMessageIndex(messages, "target"))
        assertEquals(-1, findFocusMessageIndex(messages, "missing"))
        assertTrue(isFocusMessage("target", "target"))
        assertFalse(isFocusMessage("before", "target"))
        assertFalse(isFocusMessage("", ""))
    }

    @Test
    fun pinnedAndMutedPreferencesAreWrittenToConversationCache() = runTest {
        var recorded: Triple<String, Boolean, Boolean>? = null
        val detail = ConversationDetail(
            id = "conversation-1",
            type = ConversationType.GROUP,
            title = "测试群",
            avatarText = "测",
            preferences = ConversationPreferences(isPinned = true, isMuted = true),
        )

        writeConversationPreferencesToRoom("conversation-1", detail) { id, pinned, muted ->
            recorded = Triple(id, pinned, muted)
        }

        assertEquals(Triple("conversation-1", true, true), recorded)
    }

    @Test
    fun leavingGroupDeletesLocalConversationParent() = runTest {
        val cached = ConversationEntity(
            id = "conversation-1",
            title = "测试群",
            lastMessage = "最后一条消息",
            timeLabel = "10:00",
            avatarText = "测",
            lastActiveTime = 30L,
        )
        var deleted: ConversationEntity? = null

        removeLeftConversationFromRoom(
            conversationId = "conversation-1",
            findConversation = { id -> cached.takeIf { it.id == id } },
            deleteConversation = { deleted = it },
        )

        assertEquals(cached, deleted)
    }

    private fun dto(id: String, content: String, timestamp: Long) = ChatMessageDto(
        id = id,
        conversationId = "conversation-1",
        senderId = "sender-1",
        content = content,
        timeLabel = "10:00",
        isMine = false,
        timeStamp = timestamp,
    )

    private fun domain(id: String, timestamp: Long) = ChatMessage(
        id = id,
        conversationId = "conversation-1",
        senderId = "sender-1",
        content = id,
        timeLabel = "10:00",
        isMine = false,
        timestamp = timestamp,
    )
}
