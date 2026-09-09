package com.sakuya.backend.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sakuya.backend.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证本地存储实现的读写、哈希元数据和根目录逃逸防护。 */
class LocalContentStorageTest {
    @TempDir Path root;
    @Test void putAndReadPreserveBytesAndHash() {
        LocalContentStorage storage = new LocalContentStorage(root);
        byte[] content = "内容完整性".getBytes(StandardCharsets.UTF_8);
        ContentStorage.StoredObject stored = storage.put("books/test/chapter.txt", content, "text/plain");
        assertEquals(content.length, stored.byteSize());
        assertEquals(ContentHashes.sha256(content), stored.sha256());
        assertArrayEquals(content, storage.read(stored.key()));
    }
    @Test void objectKeyCannotEscapeStorageRoot() {
        LocalContentStorage storage = new LocalContentStorage(root);
        assertThrows(BusinessException.class, () -> storage.put("../outside.txt", new byte[] {1}, "text/plain"));
    }
    @Test void relativeRootUsesTheSameEscapeProtection() {
        LocalContentStorage storage = new LocalContentStorage(Path.of("build/content-storage-test"));
        assertThrows(BusinessException.class, () -> storage.put("../outside.txt", new byte[] {1}, "text/plain"));
    }
}
