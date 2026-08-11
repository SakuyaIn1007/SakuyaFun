package com.sakuya.backend.config;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.catalog.UserLibraryBook;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.chat.ChatService;
import com.sakuya.backend.friend.FriendRequest;
import com.sakuya.backend.friend.FriendRequestRepository;
import com.sakuya.backend.friend.Friendship;
import com.sakuya.backend.friend.FriendshipRepository;
import com.sakuya.backend.feed.FeedPost;
import com.sakuya.backend.feed.FeedPostRepository;
import com.sakuya.backend.user.User;
import com.sakuya.backend.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataInitializer.java
 * 职责说明：在开发与测试环境创建演示账号，并为 sakuya 补齐资料、好友、书架、会话和好友申请数据。
 * 执行流程：应用启动且 app.seed.enabled=true -> 查找或创建演示用户与书籍 -> 按唯一关系补充关联数据 -> 不重复插入已有数据。
 */
@Configuration
public class DataInitializer {
    private static final String DEMO_PASSWORD = "password123";

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
    CommandLineRunner seed(
            UserRepository users,
            BookRepository books,
            UserLibraryRepository library,
            FriendshipRepository friendships,
            FriendRequestRepository requests,
            FeedPostRepository feedPosts,
            ChatService chat,
            PasswordEncoder encoder
    ) {
        return args -> {
            User sakuya = findOrCreateUser(users, encoder, "sakuya", "咲夜");
            configureSakuyaProfile(sakuya);
            users.save(sakuya);
            User alice = findOrCreateUser(users, encoder, "alice", "爱丽丝");
            User bob = findOrCreateUser(users, encoder, "bob", "鲍勃");
            User rin = findOrCreateUser(users, encoder, "rin", "凛");

            seedBooks(books);
            seedFriendship(friendships, sakuya, alice);
            seedFriendship(friendships, sakuya, bob);
            seedFriendship(friendships, alice, bob);
            seedLibrary(library, sakuya);
            seedConversation(chat, sakuya, alice, List.of("欢迎来到 Sakuya！", "我刚把《刀剑神域》加入书架。"));
            seedConversation(chat, sakuya, bob, List.of("周末一起聊聊最近读的书吗？"));
            seedFriendRequest(requests, rin, sakuya);
            seedFeed(feedPosts, sakuya);
        };
    }

    /** 查找既有账号或创建默认密码账号，避免每次启动生成不同的用户 ID。 */
    private User findOrCreateUser(UserRepository users, PasswordEncoder encoder, String account, String nickname) {
        return users.findByAccountIgnoreCase(account)
                .orElseGet(() -> users.save(new User(account, encoder.encode(DEMO_PASSWORD), nickname)));
    }

    /** 为 sakuya 写入完整资料页字段；调用方随后保存实体，供个人资料页面测试使用。 */
    private void configureSakuyaProfile(User sakuya) {
        sakuya.setSignature("在书页之间，寻找下一段故事。");
        sakuya.setGender(1);
        sakuya.setBirthday("2000-10-16");
        sakuya.setRegionCode("CN-31");
        sakuya.setPhoneNumber("13800138000");
        sakuya.setEmail("sakuya@example.test");
        sakuya.setPokeText("拍了拍你，并推荐了一本书");
        sakuya.setRingtoneName("星夜阅读");
        sakuya.setCanBeAddedByStrangers(true);
        sakuya.setShowProfileToStrangers(true);
        sakuya.setMuteMessagesFromUnknown(false);
    }

    /**
     * 初始化首页、书架和时间表共用书目；只补齐缺失书目，不覆盖已存在的手工资料。
     * 执行流程：启动时读取已有 ID -> 过滤已存在项目 -> 保存时间表需要的新增小说。
     */
    private void seedBooks(BookRepository books) {
        List<Book> seedBooks = List.of(
                new Book("sword-art-online", "刀剑神域", "川原砾", "电击文库", 4.8f, List.of("科幻", "冒险", "虚拟现实"), "以完全潜行游戏为舞台，讲述玩家在虚拟世界中求生与成长的冒险故事。", "recommend"),
                new Book("tensei-slime", "关于我转生变成史莱姆这档事", "伏濑", "GC Novels", 4.6f, List.of("异世界", "奇幻", "轻松"), "转生为史莱姆后建立伙伴与国家的异世界冒险。", "recommend"),
                new Book("rezero", "Re:从零开始的异世界生活", "长月达平", "MF文库J", 4.7f, List.of("异世界", "悬疑", "轮回"), "拥有死亡回归能力的少年，在反复轮回中守护重要之人。", "recommend"),
                new Book("86", "86-不存在的战区", "安里アサト", "电击文库", 4.8f, List.of("科幻", "战争", "机甲"), "被隔离的少年少女驾驶无人兵器，在战场与偏见中寻找未来。", "novel"),
                new Book("spice-and-wolf", "狼与香辛料", "支仓冻砂", "电击文库", 4.7f, List.of("冒险", "经商", "奇幻"), "旅行商人与狼神少女结伴远行。", "novel"),
                new Book("mahouka", "魔法科高校的劣等生", "佐岛勤", "电击文库", 4.4f, List.of("科幻", "校园", "魔法"), "魔法成为技术的时代，一对兄妹进入魔法高中后的故事。", "novel"),
                new Book("biblia", "古书堂事件手帖", "三上延", "MediaWorks", 4.3f, List.of("悬疑", "治愈", "日常"), "古书店店主通过书籍与线索解开旧书背后隐藏的故事。", "novel"),
                new Book("monogatari", "物语系列", "西尾维新", "讲谈社BOX", 4.6f, List.of("怪谈", "悬疑", "青春"), "少年与遭遇怪异的少女们相遇，并直面各自内心的问题。", "novel"),
                new Book("no-game-no-life", "NO GAME NO LIFE", "榎宫祐", "MF文库J", 4.5f, List.of("异世界", "智斗", "奇幻"), "天才游戏玩家兄妹来到一切由游戏决定的世界。", "novel"),
                new Book("bungo-stray-dogs", "文豪野犬", "朝雾卡夫卡", "角川Beans", 4.3f, List.of("超能力", "悬疑", "文学"), "拥有文学家之名与异能力的人们卷入城市中的多方纷争。", "novel"),
                new Book("danmachi", "在地下城寻求邂逅是否搞错了什么", "大森藤野", "GA文库", 4.5f, List.of("冒险", "奇幻", "恋爱"), "新手冒险者在迷宫都市中邂逅伙伴并不断成长。", "novel"),
                new Book("fate-zero", "Fate/Zero", "虚渊玄", "TYPE-MOON", 4.8f, List.of("圣杯战争", "黑暗", "史诗"), "七位魔术师与英灵围绕圣杯展开残酷战争。", "novel"),
                new Book("sakurasou", "樱花庄的宠物女孩", "鸭志田一", "电击文库", 4.4f, List.of("恋爱", "青春", "校园"), "住在樱花庄的少年少女，在创作、才能与青春中寻找方向。", "novel")
        );
        books.saveAll(seedBooks.stream().filter(book -> !books.existsById(book.getId())).toList());
    }

    /** 双向保存好友关系，使好友列表按当前用户 userId 查询时双方都可见。 */
    private void seedFriendship(FriendshipRepository friendships, User first, User second) {
        if (!friendships.existsByUserIdAndFriendId(first.getId(), second.getId())) friendships.save(new Friendship(first.getId(), second.getId()));
        if (!friendships.existsByUserIdAndFriendId(second.getId(), first.getId())) friendships.save(new Friendship(second.getId(), first.getId()));
    }

    /** 为 sakuya 保存不同阅读进度，供书架列表和进度展示测试使用。 */
    private void seedLibrary(UserLibraryRepository library, User sakuya) {
        seedLibraryBook(library, sakuya.getId(), "sword-art-online", 0.68f);
        seedLibraryBook(library, sakuya.getId(), "rezero", 0.25f);
        seedLibraryBook(library, sakuya.getId(), "spice-and-wolf", 1f);
    }

    private void seedLibraryBook(UserLibraryRepository library, UUID userId, String bookId, float progress) {
        if (library.existsByUserIdAndBookId(userId, bookId)) return;
        UserLibraryBook item = new UserLibraryBook(userId, bookId);
        item.setProgress(progress);
        library.save(item);
    }

    /** 为好友单聊补首批消息；已有消息时不追加，避免重启后生成重复记录。 */
    private void seedConversation(ChatService chat, User owner, User friend, List<String> messages) {
        ChatService.ConversationDto conversation = chat.direct(owner.getId(), friend.getId());
        UUID conversationId = UUID.fromString(conversation.id());
        if (!chat.messages(owner.getId(), conversationId, null, 1).isEmpty()) return;
        for (String message : messages) chat.save(friend.getId(), conversationId, message, "text");
    }

    /** 保留一条凛发给 sakuya 的待处理申请，用于测试好友申请列表与接受流程。 */
    private void seedFriendRequest(FriendRequestRepository requests, User sender, User receiver) {
        if (!requests.existsBySenderIdAndReceiverIdAndStatus(sender.getId(), receiver.getId(), FriendRequest.Status.PENDING)) {
            requests.save(new FriendRequest(sender.getId(), receiver.getId(), "一起交流最近读的轻小说吧"));
        }
    }
    /** 数据库存放演示动态，客户端不再硬编码月见草等测试内容。 */
    private void seedFeed(FeedPostRepository posts, User author) {
        if (posts.count() > 0) return;
        posts.save(new FeedPost(author.getId(), "雨天读完《山茶文具店》", "像收到一封温柔的信。书里那些替人写下的话，让这个潮湿的下午也慢慢安静下来。", "#读书笔记|#治愈系"));
        posts.save(new FeedPost(author.getId(), "给八月列了一张待读清单", "想把阅读的速度放慢一点，留些空白给散步和发呆。", "#待读书单|#八月阅读"));
    }
}
