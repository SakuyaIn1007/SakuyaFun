package com.sakuya.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users", indexes = @Index(name = "idx_user_account", columnList = "account", unique = true))
public class User {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 64) private String account;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false, length = 40) private String nickname;
    private String avatarUrl = "";
    private String signature;
    private Integer gender;
    private String birthday;
    private String regionCode;
    private String phoneNumber;
    private String email;
    private String pokeText;
    private String ringtoneName;
    private boolean canBeAddedByStrangers = true;
    private boolean showProfileToStrangers = true;
    /** 是否允许陌生人查看自己的关注和粉丝列表；仅影响关系列表，不影响个人资料页。 */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean showFollowLists = true;
    private boolean muteMessagesFromUnknown = false;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected User() {}
    public User(String account, String passwordHash, String nickname) {
        this.id = UUID.randomUUID(); this.account = account; this.passwordHash = passwordHash;
        this.nickname = nickname; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getAccount() { return account; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getSignature() { return signature; }
    public Integer getGender() { return gender; }
    public String getBirthday() { return birthday; }
    public String getRegionCode() { return regionCode; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getPokeText() { return pokeText; }
    public String getRingtoneName() { return ringtoneName; }
    public boolean isCanBeAddedByStrangers() { return canBeAddedByStrangers; }
    public boolean isShowProfileToStrangers() { return showProfileToStrangers; }
    public boolean isShowFollowLists() { return showFollowLists; }
    public boolean isMuteMessagesFromUnknown() { return muteMessagesFromUnknown; }
    public void setAvatarUrl(String value) { avatarUrl = value == null ? "" : value; }
    public void setNickname(String value) { nickname = value; }
    public void setSignature(String value) { signature = value; }
    public void setGender(Integer value) { gender = value; }
    public void setBirthday(String value) { birthday = value; }
    public void setRegionCode(String value) { regionCode = value; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public void setEmail(String value) { email = value; }
    public void setPokeText(String value) { pokeText = value; }
    public void setRingtoneName(String value) { ringtoneName = value; }
    public void setCanBeAddedByStrangers(boolean value) { canBeAddedByStrangers = value; }
    public void setShowProfileToStrangers(boolean value) { showProfileToStrangers = value; }
    public void setShowFollowLists(boolean value) { showFollowLists = value; }
    public void setMuteMessagesFromUnknown(boolean value) { muteMessagesFromUnknown = value; }
}
