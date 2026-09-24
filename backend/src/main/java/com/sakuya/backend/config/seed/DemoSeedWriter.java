package com.sakuya.backend.config.seed;

import com.sakuya.backend.catalog.UserLibraryBook;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.chat.ChatService;
import com.sakuya.backend.feed.FeedComment;
import com.sakuya.backend.feed.FeedCommentRepository;
import com.sakuya.backend.feed.FeedEngagement;
import com.sakuya.backend.feed.FeedEngagementRepository;
import com.sakuya.backend.feed.FeedPost;
import com.sakuya.backend.feed.FeedPostRepository;
import com.sakuya.backend.friend.FriendRequest;
import com.sakuya.backend.friend.FriendRequestRepository;
import com.sakuya.backend.friend.Friendship;
import com.sakuya.backend.friend.FriendshipRepository;
import com.sakuya.backend.social.Follow;
import com.sakuya.backend.social.FollowRepository;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DemoSeedWriter.java
 * 职责说明：把校验过的 SeedData 幂等地写入数据库，是种子数据唯一的落库入口。
 * 执行流程：按 accounts -> friendships -> follows -> friendRequests -> library -> conversations -> posts 顺序逐条写入，
 * 每一条都先按自然键判断是否已存在，只有缺失才插入。
 * 幂等约定：本类不使用任何「表里有没有数据」这类全局计数守卫。旧实现用 posts.count() > 0 判断是否导入动态，
 * 导致库里只要有一条用户动态，全部种子动态就被静默跳过；逐行按自然键判断才能既补齐缺失、又不重复插入。
 */
@Component
public class DemoSeedWriter {
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository users;
    private final UserLibraryRepository library;
    private final FriendshipRepository friendships;
    private final FollowRepository follows;
    private final FriendRequestRepository friendRequests;
    private final FeedPostRepository posts;
    private final FeedCommentRepository comments;
    private final FeedEngagementRepository engagements;
    private final ChatService chat;
    private final PasswordEncoder encoder;

    public DemoSeedWriter(
        UserRepository users,
        UserLibraryRepository library,
        FriendshipRepository friendships,
        FollowRepository follows,
        FriendRequestRepository friendRequests,
        FeedPostRepository posts,
        FeedCommentRepository comments,
        FeedEngagementRepository engagements,
        ChatService chat,
        PasswordEncoder encoder
    ) {
        this.users = users;
        this.library = library;
        this.friendships = friendships;
        this.follows = follows;
        this.friendRequests = friendRequests;
        this.posts = posts;
        this.comments = comments;
        this.engagements = engagements;
        this.chat = chat;
        this.encoder = encoder;
    }

    /** 写入全部种子数据，返回本次实际新增的动态数，供启动日志说明幂等结果。 */
    public int write(SeedData data) {
        Map<String, User> accounts = seedAccounts(data.accounts());
        seedFriendships(data.friendships(), accounts);
        seedFollows(data.follows(), accounts);
        seedFriendRequests(data.friendRequests(), accounts);
        seedLibrary(data.library(), accounts);
        seedConversations(data.conversations(), accounts);
        return seedPosts(data.posts(), accounts);
    }

    /** 查找既有账号或创建默认密码账号，避免每次启动生成不同的用户 ID。 */
    private Map<String, User> seedAccounts(List<SeedData.Account> seeds) {
        Map<String, User> result = new HashMap<>();
        for (SeedData.Account seed : nullSafe(seeds)) {
            User user = users.findByAccountIgnoreCase(seed.account())
                .orElseGet(() -> users.save(new User(seed.account(), encoder.encode(DEMO_PASSWORD), seed.nickname())));
            applyProfile(user, seed);
            result.put(seed.account(), users.save(user));
        }
        return result;
    }

    /**
     * 按 YAML 补齐资料字段。只覆盖 YAML 中显式给出的字段，
     * 未声明的字段保持数据库现状，避免每次启动把用户手工改过的资料重置回默认值。
     */
    private void applyProfile(User user, SeedData.Account seed) {
        // 昵称以 YAML 为准：演示账号的昵称会被接口断言直接引用，必须与配置一致。
        if (seed.nickname() != null) user.setNickname(seed.nickname());
        SeedData.Profile profile = seed.profile();
        if (profile == null) return;
        if (profile.signature() != null) user.setSignature(profile.signature());
        if (profile.gender() != null) user.setGender(profile.gender());
        if (profile.birthday() != null) user.setBirthday(profile.birthday());
        if (profile.regionCode() != null) user.setRegionCode(profile.regionCode());
        if (profile.phoneNumber() != null) user.setPhoneNumber(profile.phoneNumber());
        if (profile.email() != null) user.setEmail(profile.email());
        if (profile.pokeText() != null) user.setPokeText(profile.pokeText());
        if (profile.ringtoneName() != null) user.setRingtoneName(profile.ringtoneName());
        if (profile.canBeAddedByStrangers() != null) user.setCanBeAddedByStrangers(profile.canBeAddedByStrangers());
        if (profile.showProfileToStrangers() != null) user.setShowProfileToStrangers(profile.showProfileToStrangers());
        if (profile.muteMessagesFromUnknown() != null) user.setMuteMessagesFromUnknown(profile.muteMessagesFromUnknown());
    }

    /** 双向保存好友关系，使好友列表按当前用户 userId 查询时双方都可见。 */
    private void seedFriendships(List<SeedData.FriendshipSeed> seeds, Map<String, User> accounts) {
        for (SeedData.FriendshipSeed seed : nullSafe(seeds)) {
            UUID first = accounts.get(seed.a()).getId();
            UUID second = accounts.get(seed.b()).getId();
            if (!friendships.existsByUserIdAndFriendId(first, second)) friendships.save(new Friendship(first, second));
            if (!friendships.existsByUserIdAndFriendId(second, first)) friendships.save(new Friendship(second, first));
        }
    }

    /** 单向关注关系；已存在时重复调用是安全的，但这里仍先判断，避免无意义的主键写入。 */
    private void seedFollows(List<SeedData.FollowSeed> seeds, Map<String, User> accounts) {
        for (SeedData.FollowSeed seed : nullSafe(seeds)) {
            UUID follower = accounts.get(seed.follower()).getId();
            UUID following = accounts.get(seed.following()).getId();
            if (!follows.existsByFollowerIdAndFollowingId(follower, following)) {
                follows.save(new Follow(follower, following));
            }
        }
    }

    private void seedFriendRequests(List<SeedData.FriendRequestSeed> seeds, Map<String, User> accounts) {
        for (SeedData.FriendRequestSeed seed : nullSafe(seeds)) {
            UUID sender = accounts.get(seed.from()).getId();
            UUID receiver = accounts.get(seed.to()).getId();
            if (!friendRequests.existsBySenderIdAndReceiverIdAndStatus(sender, receiver, FriendRequest.Status.PENDING)) {
                friendRequests.save(new FriendRequest(sender, receiver, seed.message()));
            }
        }
    }

    private void seedLibrary(List<SeedData.LibrarySeed> seeds, Map<String, User> accounts) {
        for (SeedData.LibrarySeed seed : nullSafe(seeds)) {
            UUID userId = accounts.get(seed.account()).getId();
            if (library.existsByUserIdAndBookId(userId, seed.bookId())) continue;
            UserLibraryBook item = new UserLibraryBook(userId, seed.bookId());
            if (seed.progress() != null) item.setProgress(seed.progress());
            library.save(item);
        }
    }

    /** 为好友单聊补首批消息；已有消息时不追加，避免重启后生成重复记录。 */
    private void seedConversations(List<SeedData.ConversationSeed> seeds, Map<String, User> accounts) {
        for (SeedData.ConversationSeed seed : nullSafe(seeds)) {
            List<User> participants = nullSafe(seed.participants()).stream().map(accounts::get).toList();
            if (participants.size() < 2) continue;
            ChatService.ConversationDto conversation = chat.direct(participants.get(0).getId(), participants.get(1).getId());
            UUID conversationId = UUID.fromString(conversation.id());
            if (!chat.messages(participants.get(0).getId(), conversationId, null, 1).isEmpty()) continue;
            for (SeedData.MessageSeed message : nullSafe(seed.messages())) {
                chat.save(accounts.get(message.from()).getId(), conversationId, message.content(), "text");
            }
        }
    }

    /**
     * 写入动态、点赞、收藏与评论。
     * 执行流程：逐条动态 -> 按 (作者, 标题) 取回既有动态或新建 -> 无论新旧都补齐互动与评论。
     * 关键点：动态已存在时**不能**直接跳过。老版本种子导入过同名动态却没有互动数据，
     * 若在这里 continue，YAML 里声明的点赞和评论就永远补不上，表现为「动态有了但一个赞都没有」。
     * 互动与评论各自按自然键幂等，因此对新旧动态统一执行是安全的。
     * 评论发布时间以动态实际的 createdAt 为基准，避免既有旧动态配出「评论早于动态」的错乱时间。
     * 返回新增动态数，便于启动日志区分「首次导入」与「已是最新」。
     */
    private int seedPosts(List<SeedData.PostSeed> seeds, Map<String, User> accounts) {
        int created = 0;
        for (SeedData.PostSeed seed : nullSafe(seeds)) {
            User author = accounts.get(seed.author());
            FeedPost existing = posts.findFirstByUserIdAndTitle(author.getId(), seed.title()).orElse(null);
            FeedPost post = existing != null ? existing : posts.save(new FeedPost(
                author.getId(), seed.title(), seed.content(), String.join("|", nullSafe(seed.tags())),
                relativeTime(seed.daysAgo(), seed.hoursAgo())));
            if (existing == null) created++;
            seedEngagements(post, seed, accounts);
            seedComments(post, seed, accounts, post.getCreatedAt());
        }
        return created;
    }

    private void seedEngagements(FeedPost post, SeedData.PostSeed seed, Map<String, User> accounts) {
        for (String account : nullSafe(seed.likes())) addEngagement(post.getId(), accounts.get(account).getId(), FeedEngagement.Type.LIKE);
        for (String account : nullSafe(seed.favorites())) addEngagement(post.getId(), accounts.get(account).getId(), FeedEngagement.Type.FAVORITE);
    }

    private void addEngagement(UUID postId, UUID userId, FeedEngagement.Type type) {
        if (engagements.existsByPostIdAndUserIdAndType(postId, userId, type)) return;
        engagements.save(new FeedEngagement(postId, userId, type));
    }

    /**
     * 写入一条动态的评论。
     * 执行流程：先一次性读出该动态的既有评论建立 (作者, 内容) -> ID 索引 -> 逐个处理种子评论：
     * 已存在的直接复用其 ID，缺失的插入。无论走哪个分支都把 ID 追加进 ids，
     * 这样 replyTo 的列表下标在「部分已存在」的场景下也不会错位。
     */
    private void seedComments(FeedPost post, SeedData.PostSeed seed, Map<String, User> accounts, Instant publishedAt) {
        Map<String, UUID> existing = new HashMap<>();
        for (FeedComment comment : comments.findByPostIdOrderByCreatedAtAsc(post.getId(), PageRequest.of(0, 200)).getContent()) {
            existing.putIfAbsent(key(comment.getAuthorId(), comment.getContent()), comment.getId());
        }
        List<UUID> ids = new ArrayList<>();
        List<SeedData.CommentSeed> seeds = nullSafe(seed.comments());
        for (int index = 0; index < seeds.size(); index++) {
            SeedData.CommentSeed commentSeed = seeds.get(index);
            UUID commentAuthor = accounts.get(commentSeed.author()).getId();
            UUID already = existing.get(key(commentAuthor, commentSeed.content()));
            if (already != null) {
                ids.add(already);
                continue;
            }
            UUID replyTo = commentSeed.replyTo() == null ? null : ids.get(commentSeed.replyTo());
            FeedComment saved = comments.save(new FeedComment(
                post.getId(), commentAuthor, commentSeed.content(), replyTo, commentTime(commentSeed, publishedAt)));
            ids.add(saved.getId());
        }
    }

    /** 评论自然键，与 existsByPostIdAndAuthorIdAndContent 的语义保持一致。 */
    private String key(UUID authorId, String content) {
        return authorId + " " + content;
    }

    /** 评论时间取「动态发布时间 + 相对偏移」，保证严格晚于动态本身。 */
    private Instant commentTime(SeedData.CommentSeed seed, Instant publishedAt) {
        Instant offset = publishedAt.plus(offsetMinutes(seed.daysAgo(), seed.hoursAgo()), ChronoUnit.MINUTES);
        Instant now = Instant.now();
        return offset.isAfter(now) ? now : offset;
    }

    /** 把 daysAgo / hoursAgo 换算为距今的时间点；两者都为空时返回当前时间。 */
    private Instant relativeTime(Integer daysAgo, Integer hoursAgo) {
        return Instant.now().minus(offsetMinutes(daysAgo, hoursAgo), ChronoUnit.MINUTES);
    }

    private long offsetMinutes(Integer daysAgo, Integer hoursAgo) {
        return (daysAgo == null ? 0L : daysAgo * 24L * 60L) + (hoursAgo == null ? 0L : hoursAgo * 60L);
    }

    private static <T> List<T> nullSafe(List<T> value) {
        return value == null ? List.of() : value;
    }
}
