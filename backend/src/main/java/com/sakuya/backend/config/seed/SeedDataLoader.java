package com.sakuya.backend.config.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * SeedDataLoader.java
 * 职责说明：读取 seed/demo-data.yml，反序列化为 SeedData，并在写库前完成引用完整性校验。
 * 执行流程：读 classpath 资源 -> Jackson YAML 反序列化 -> validate 逐项校验 -> 返回 SeedData。
 * 设计说明：校验失败一律抛异常让应用启动失败。种子数据的错误（账号拼错、下标越界）
 * 如果被静默忽略，表现为「界面上少了几条数据」，排查成本远高于直接启动失败。
 */
@Component
public class SeedDataLoader {
    private static final String RESOURCE_PATH = "seed/demo-data.yml";

    /**
     * FAIL_ON_UNKNOWN_PROPERTIES 保持 Jackson 默认的开启状态：YAML 里写错字段名时立刻失败，
     * 而不是被静默丢弃后表现为「界面上少了几条数据」。
     */
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    /** 加载并校验种子数据；任何问题都会抛出 IllegalStateException 中断应用启动。 */
    public SeedData load() {
        SeedData data;
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            data = yaml.readValue(in, SeedData.class);
        } catch (IOException e) {
            throw new IllegalStateException("读取种子数据失败：" + RESOURCE_PATH, e);
        }
        validate(data);
        return data;
    }

    /**
     * 校验引用完整性。检查项与原因：
     * 1. account 非空且不重复 —— 账号是其他所有条目的外键，重复会让引用产生歧义。
     * 2. 所有引用的 account 必须已声明 —— 拼错账号名是最容易犯的错。
     * 3. replyTo 必须指向列表中更早的评论 —— 保证创建时被回复评论已经存在，同时避免自引用。
     * 4. 相对时间非负、进度在 [0,1] —— 负数时间会生成未来动态，进度越界会被实体静默截断。
     * 5. 好友与申请的双方不能是同一人 —— 自我关系没有业务含义。
     */
    private void validate(SeedData data) {
        List<SeedData.Account> accounts = nullSafe(data.accounts());
        Set<String> known = new HashSet<>();
        for (int i = 0; i < accounts.size(); i++) {
            SeedData.Account account = accounts.get(i);
            if (account == null || isBlank(account.account())) {
                throw new IllegalStateException("accounts[" + i + "].account 不能为空");
            }
            if (isBlank(account.nickname())) {
                throw new IllegalStateException("账号 " + account.account() + " 缺少 nickname");
            }
            if (!known.add(account.account())) {
                throw new IllegalStateException("账号重复声明：" + account.account());
            }
        }

        for (SeedData.FriendshipSeed item : nullSafe(data.friendships())) {
            requireKnown(known, item.a(), "friendships 的 a");
            requireKnown(known, item.b(), "friendships 的 b");
            if (item.a().equals(item.b())) {
                throw new IllegalStateException("好友关系的双方不能是同一账号：" + item.a());
            }
        }
        for (SeedData.FollowSeed item : nullSafe(data.follows())) {
            requireKnown(known, item.follower(), "follows 的 follower");
            requireKnown(known, item.following(), "follows 的 following");
            if (item.follower().equals(item.following())) {
                throw new IllegalStateException("不能关注自己：" + item.follower());
            }
        }
        for (SeedData.FriendRequestSeed item : nullSafe(data.friendRequests())) {
            requireKnown(known, item.from(), "friendRequests 的 from");
            requireKnown(known, item.to(), "friendRequests 的 to");
            if (item.from().equals(item.to())) {
                throw new IllegalStateException("不能给自己发好友申请：" + item.from());
            }
        }
        for (SeedData.LibrarySeed item : nullSafe(data.library())) {
            requireKnown(known, item.account(), "library 的 account");
            Float progress = item.progress();
            if (progress != null && (progress < 0f || progress > 1f)) {
                throw new IllegalStateException("library 的 progress 必须在 0 到 1 之间：" + item.account() + "/" + item.bookId());
            }
        }
        for (SeedData.ConversationSeed item : nullSafe(data.conversations())) {
            List<String> participants = nullSafe(item.participants());
            if (participants.isEmpty()) {
                throw new IllegalStateException("conversations 的 participants 不能为空");
            }
            for (String account : participants) {
                requireKnown(known, account, "conversations 的 participants");
            }
            for (SeedData.MessageSeed message : nullSafe(item.messages())) {
                requireKnown(participants, message.from(), "conversations.messages 的 from（必须是本会话参与者）");
            }
        }

        for (int i = 0; i < nullSafe(data.posts()).size(); i++) {
            SeedData.PostSeed post = data.posts().get(i);
            String where = "posts[" + i + "]（" + post.title() + "）";
            requireKnown(known, post.author(), where + " 的 author");
            requireNonNegative(post.daysAgo(), where + " 的 daysAgo");
            requireNonNegative(post.hoursAgo(), where + " 的 hoursAgo");
            for (String account : nullSafe(post.likes())) {
                requireKnown(known, account, where + " 的 likes");
            }
            for (String account : nullSafe(post.favorites())) {
                requireKnown(known, account, where + " 的 favorites");
            }
            List<SeedData.CommentSeed> comments = nullSafe(post.comments());
            for (int j = 0; j < comments.size(); j++) {
                SeedData.CommentSeed comment = comments.get(j);
                String commentWhere = where + " 的 comments[" + j + "]";
                requireKnown(known, comment.author(), commentWhere + " 的 author");
                requireNonNegative(comment.daysAgo(), commentWhere + " 的 daysAgo");
                requireNonNegative(comment.hoursAgo(), commentWhere + " 的 hoursAgo");
                Integer replyTo = comment.replyTo();
                // 只允许指向更早的下标：既保证被回复评论已经创建，也顺带排除了自引用。
                if (replyTo != null && (replyTo < 0 || replyTo >= j)) {
                    throw new IllegalStateException(
                        commentWhere + " 的 replyTo=" + replyTo + " 非法，只能指向列表中更早的评论（0 到 " + (j - 1) + "）");
                }
            }
        }
    }

    /** 校验账号已在 accounts 中声明；会话消息的 from 则改为限定在本会话参与者范围内。 */
    private void requireKnown(Collection<String> scope, String account, String field) {
        if (account == null || !scope.contains(account)) {
            throw new IllegalStateException(field + " 引用了未声明的账号：" + account);
        }
    }

    private void requireNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new IllegalStateException(field + " 不能为负数：" + value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> nullSafe(List<T> value) {
        return value == null ? List.of() : value;
    }
}
