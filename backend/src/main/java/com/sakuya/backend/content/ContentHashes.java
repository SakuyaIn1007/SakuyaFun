package com.sakuya.backend.content;

import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * ContentHashes.java
 * 职责说明：内容对象（正文、章节、封面）的 SHA-256 计算，供存储层与业务层共用一份实现。
 * 执行流程：调用方传入原始字节 -> 返回小写十六进制摘要。
 *
 * 从 ContentStorageConfiguration 抽出为顶层类：封面预取在写入存储前需要用它做
 * 「内容未变则跳过重写」的判定，而该判定发生在 catalog 包内，无法访问包私有实现。
 */
public final class ContentHashes {

    private ContentHashes() { }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 不可用", error);
        }
    }
}
