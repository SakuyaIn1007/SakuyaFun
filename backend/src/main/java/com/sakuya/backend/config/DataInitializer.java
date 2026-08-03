package com.sakuya.backend.config;

import com.sakuya.backend.catalog.*; import com.sakuya.backend.chat.*; import com.sakuya.backend.friend.*; import com.sakuya.backend.user.*; import java.util.*; import org.springframework.boot.CommandLineRunner; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class DataInitializer {
 @Bean @ConditionalOnProperty(name="app.seed.enabled",havingValue="true") CommandLineRunner seed(UserRepository users,BookRepository books,FriendshipRepository friendships,ChatService chat,PasswordEncoder encoder){return args->{
  User alice=users.findByAccountIgnoreCase("alice").orElseGet(()->users.save(new User("alice",encoder.encode("password123"),"爱丽丝")));User bob=users.findByAccountIgnoreCase("bob").orElseGet(()->users.save(new User("bob",encoder.encode("password123"),"鲍勃")));
  if(!friendships.existsByUserIdAndFriendId(alice.getId(),bob.getId()))friendships.save(new Friendship(alice.getId(),bob.getId()));if(!friendships.existsByUserIdAndFriendId(bob.getId(),alice.getId()))friendships.save(new Friendship(bob.getId(),alice.getId()));
  if(books.count()==0)books.saveAll(List.of(
   new Book("sword-art-online","刀剑神域","川原砾","电击文库",4.8f,List.of("科幻","冒险","虚拟现实"),"以完全潜行游戏为舞台，讲述玩家在虚拟世界中求生与成长的冒险故事。","recommend"),
   new Book("tensei-slime","关于我转生变成史莱姆这档事","伏濑","GC Novels",4.6f,List.of("异世界","奇幻","轻松"),"转生为史莱姆后建立伙伴与国家的异世界冒险。","recommend"),
   new Book("rezero","Re:从零开始的异世界生活","长月达平","MF文库J",4.7f,List.of("异世界","悬疑","轮回"),"拥有死亡回归能力的少年，在反复轮回中守护重要之人。","recommend"),
   new Book("86","86-不存在的战区","安里アサト","电击文库",4.8f,List.of("科幻","战争","机甲"),"被隔离的少年少女驾驶无人兵器，在战场与偏见中寻找未来。","novel"),
   new Book("spice-and-wolf","狼与香辛料","支仓冻砂","电击文库",4.7f,List.of("冒险","经商","奇幻"),"旅行商人与狼神少女结伴远行。","novel")));
  if(chat.conversations(alice.getId()).isEmpty()){Conversation c=chat.create("爱丽丝与鲍勃",List.of(alice.getId(),bob.getId()));chat.save(bob.getId(),c.getId(),"欢迎来到 Sakuya！","text");}
 };}
}
