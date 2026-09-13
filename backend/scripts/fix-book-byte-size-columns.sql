-- 修复 books 表中两个字节数列的可空性问题。
--
-- 背景：cover_byte_size 与 full_content_byte_size 是后加字段，早期由 ddl-auto=update
-- 以 `bigint DEFAULT NULL` 建到已有表上，历史行全部为 NULL。Book 实体里这两个字段是
-- Java 基本类型 long，无法接收 null，Hibernate 加载实体时会抛：
--   IllegalArgumentException: Can not set long field ... to null value
-- 症状是 /library、/home/**、/books/{id} 全部返回 500，而不碰 Book 实体的接口正常。
--
-- ddl-auto=update 只会新增列，永远不会把已有列收紧为 NOT NULL，因此必须手动执行本脚本。
-- 脚本可重复执行：UPDATE 只影响仍为 NULL 的行，ALTER 重复执行结果一致。
--
-- 用法：mysql -uroot -p sakuya < backend/scripts/fix-book-byte-size-columns.sql

-- 顺序不可颠倒：严格模式下直接 MODIFY 为 NOT NULL 会因现存 NULL 报
-- "Invalid use of NULL value"（错误码 1138），所以先归零再收紧。
UPDATE books SET cover_byte_size = 0 WHERE cover_byte_size IS NULL;
UPDATE books SET full_content_byte_size = 0 WHERE full_content_byte_size IS NULL;

ALTER TABLE books MODIFY COLUMN cover_byte_size bigint NOT NULL DEFAULT 0;
ALTER TABLE books MODIFY COLUMN full_content_byte_size bigint NOT NULL DEFAULT 0;
