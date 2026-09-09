package com.sakuya.backend.content;

/**
 * ContentStorage.java
 * 职责说明：隔离正文与封面的实际存储位置，使业务层只持有对象键和完整性元数据。
 * 执行流程：导入服务写入字节 -> Storage 计算 SHA-256 并返回描述 -> 读取服务按对象键取回。
 */
public interface ContentStorage {
    StoredObject put(String key, byte[] content, String contentType);
    byte[] read(String key);
    boolean exists(String key);
    record StoredObject(String key, long byteSize, String sha256) { }
}
