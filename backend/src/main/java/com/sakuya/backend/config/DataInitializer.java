package com.sakuya.backend.config;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.config.seed.DemoSeedWriter;
import com.sakuya.backend.config.seed.SeedData;
import com.sakuya.backend.config.seed.SeedDataLoader;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DataInitializer.java
 * 职责说明：在开发与测试环境触发演示数据导入，本身只做编排，不承载任何数据内容。
 * 执行流程：应用启动且 app.seed.enabled=true -> 初始化书目 -> SeedDataLoader 读取并校验 seed/demo-data.yml
 * -> DemoSeedWriter 按自然键幂等写入 -> 记录本次新增数量。
 * 数据维护：账号、好友、关注、申请、书架、会话、动态与评论全部在 seed/demo-data.yml 中维护，
 * 增删演示内容不需要改动 Java 代码。
 */
@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
    CommandLineRunner seed(BookRepository books, SeedDataLoader loader, DemoSeedWriter writer) {
        return args -> {
            seedBooks(books);
            SeedData data = loader.load();
            int created = writer.write(data);
            // 明确区分「首次导入」与「已是最新」，便于判断幂等是否生效。
            log.info("演示数据导入完成：本次新增动态 {} 条，配置账号 {} 个。", created, data.accounts().size());
        };
    }

    /**
     * 初始化首页、书架和时间表共用书目；只补齐缺失书目，不覆盖已存在的手工资料。
     * 说明：书目属于目录数据而非社交演示数据，与动态/好友的维护节奏不同，因此保留在 Java 中。
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
}
