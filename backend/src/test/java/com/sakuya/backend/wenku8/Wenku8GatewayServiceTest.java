package com.sakuya.backend.wenku8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sakuya.backend.common.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Wenku8GatewayServiceTest.java
 * 职责说明：验证 Python 全文 DTO 到后端稳定连续阅读 DTO 的映射和异常边界。
 * 执行流程：Mock 内网 AdapterClient 返回数据 -> GatewayService 规范字段 -> 断言客户端不会接收到原始 Map。
 */
class Wenku8GatewayServiceTest {
    @Test
    void fullContentMapsDocumentAndAnchor() {
        Wenku8AdapterClient client = mock(Wenku8AdapterClient.class);
        when(client.getFullContent("/novels/471/full-content")).thenReturn(Map.of(
            "novelId", "471",
            "title", "测试小说",
            "content", "第一卷 第一章\n正文",
            "chapters", List.of(Map.of("chapterId", "100", "title", "第一章", "volumeTitle", "第一卷", "offset", 0))
        ));

        Wenku8GatewayService.FullContentDocument document = new Wenku8GatewayService(client).fullContent("471");

        assertEquals("471", document.novelId());
        assertEquals("测试小说", document.title());
        assertEquals("正文", document.content().substring(document.content().length() - 2));
        assertEquals(new Wenku8GatewayService.ChapterAnchor("100", "第一章", "第一卷", 0), document.chapters().getFirst());
    }

    @Test
    void fullContentRejectsMalformedInternalPayload() {
        Wenku8AdapterClient client = mock(Wenku8AdapterClient.class);
        when(client.getFullContent("/novels/471/full-content")).thenReturn(Map.of("title", "测试小说", "content", ""));

        BusinessException error = assertThrows(BusinessException.class, () -> new Wenku8GatewayService(client).fullContent("471"));

        assertEquals(503, error.getCode());
    }
}
