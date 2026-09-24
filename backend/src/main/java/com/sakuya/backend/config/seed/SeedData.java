package com.sakuya.backend.config.seed;

import java.util.List;

/**
 * SeedData.java
 * 职责说明：与 seed/demo-data.yml 一一对应的纯数据结构，只承载字段，不含任何行为。
 * 执行流程：SeedDataLoader 把 YAML 反序列化成本记录 -> 校验 -> DemoSeedWriter 据此写库。
 * 设计说明：
 * 1. 不使用 UUID：账号之间用 account 字符串互相引用，实体 ID 由各自构造函数生成，
 *    这样 YAML 可以手写、可读，也不需要在多处同步 UUID。
 * 2. 不使用绝对时间：动态和评论用 daysAgo / hoursAgo 描述相对发布时间，
 *    无论哪一天启动都能得到一份「最近两周」的鲜活数据。
 * 3. 回复评论用列表内索引（replyTo），而不是评论 ID，理由同上。
 */
public record SeedData(
    List<Account> accounts,
    List<FriendshipSeed> friendships,
    List<FollowSeed> follows,
    List<FriendRequestSeed> friendRequests,
    List<LibrarySeed> library,
    List<ConversationSeed> conversations,
    List<PostSeed> posts
) {
    /** 单个演示账号；profile 为空时只创建账号本身。 */
    public record Account(String account, String nickname, Profile profile) {}

    /** 账号资料字段，全部可选；留空表示保持数据库现有值。 */
    public record Profile(
        String signature,
        Integer gender,
        String birthday,
        String regionCode,
        String phoneNumber,
        String email,
        String pokeText,
        String ringtoneName,
        Boolean canBeAddedByStrangers,
        Boolean showProfileToStrangers,
        Boolean muteMessagesFromUnknown
    ) {}

    /** 好友关系；写库时会自动建立 a→b 与 b→a 两条记录。 */
    public record FriendshipSeed(String a, String b) {}

    /** 单向关注关系，follower 关注 following，喂给「关注」动态流。 */
    public record FollowSeed(String follower, String following) {}

    /** 待处理好友申请；receiver 的申请列表第一条会变成它，注意别覆盖测试依赖的那条。 */
    public record FriendRequestSeed(String from, String to, String message) {}

    /** 书架条目，bookId 必须是 seedBooks 已建好的书目 ID。 */
    public record LibrarySeed(String account, String bookId, Float progress) {}

    public record ConversationSeed(List<String> participants, List<MessageSeed> messages) {}

    /** 单条聊天消息；from 必须是 participants 之一。 */
    public record MessageSeed(String from, String content) {}

    /**
     * 一条动态。
     * daysAgo / hoursAgo 共同决定发布时间，两者都缺省时按「刚刚」处理。
     * 评论按列表顺序创建，因此 replyTo 只能指向列表中更早的评论。
     */
    public record PostSeed(
        String author,
        String title,
        String content,
        List<String> tags,
        Integer daysAgo,
        Integer hoursAgo,
        List<String> likes,
        List<String> favorites,
        List<CommentSeed> comments
    ) {}

    /** 一条评论；replyTo 是本动态评论列表中的下标，指向被回复的评论。 */
    public record CommentSeed(
        String author,
        String content,
        Integer replyTo,
        Integer daysAgo,
        Integer hoursAgo
    ) {}
}
