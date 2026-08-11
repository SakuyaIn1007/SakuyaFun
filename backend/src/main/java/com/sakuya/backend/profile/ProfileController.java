package com.sakuya.backend.profile;

import com.sakuya.backend.common.*;
import com.sakuya.backend.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserRepository users; private final Path uploadDir;
    public ProfileController(UserRepository users, @Value("${app.upload-dir}") String uploadDir) { this.users = users; this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize(); }
    @GetMapping ApiResponse<ProfileDto> get(Authentication auth) { return ApiResponse.ok(ProfileDto.from(current(auth))); }
    @PutMapping @Transactional ApiResponse<ProfileDto> update(Authentication auth, @Valid @RequestBody UpdateProfileRequest r) {
        User u = current(auth); u.setAvatarUrl(r.avatarUrl()); u.setNickname(r.nickname()); u.setSignature(r.signature()); u.setGender(r.gender());
        u.setBirthday(r.birthday()); u.setRegionCode(r.regionCode()); u.setPhoneNumber(r.phoneNumber()); u.setEmail(r.email());
        u.setPokeText(r.pokeText()); u.setRingtoneName(r.ringtoneName());
        if (r.privacySettings() != null) { u.setCanBeAddedByStrangers(r.privacySettings().canBeAddedByStrangers()); u.setShowProfileToStrangers(r.privacySettings().showProfileToStrangers()); u.setMuteMessagesFromUnknown(r.privacySettings().muteMessagesFromUnknown()); if (r.privacySettings().showFollowLists() != null) u.setShowFollowLists(r.privacySettings().showFollowLists()); }
        return ApiResponse.ok(ProfileDto.from(u));
    }
    @PostMapping("/upload/avatar") ApiResponse<String> avatar(Authentication auth, @RequestPart(value="image", required=false) MultipartFile image, @RequestPart(value="file", required=false) MultipartFile file) throws IOException {
        MultipartFile upload = image != null ? image : file;
        if (upload == null || upload.isEmpty()) throw new BusinessException(400, "请选择头像文件");
        String type = upload.getContentType(); if (type == null || !type.startsWith("image/")) throw new BusinessException(400, "仅支持图片文件");
        Files.createDirectories(uploadDir); String ext = type.contains("png") ? ".png" : type.contains("webp") ? ".webp" : ".jpg";
        String name = AuthSupport.userId(auth) + "-" + UUID.randomUUID() + ext; Files.copy(upload.getInputStream(), uploadDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/").path(name).toUriString(); User u = current(auth); u.setAvatarUrl(url); users.save(u); return ApiResponse.ok(url);
    }
    private User current(Authentication auth) { return users.findById(AuthSupport.userId(auth)).orElseThrow(() -> new BusinessException(404, "用户不存在")); }
    /** 当前用户的隐私设置；showFollowLists 为关注/粉丝列表的可见性开关。 */
    public record PrivacySettingsDto(boolean canBeAddedByStrangers, boolean showProfileToStrangers, boolean muteMessagesFromUnknown, boolean showFollowLists) {}
    /** 更新隐私设置时，showFollowLists 使用可空类型以兼容旧客户端未传该字段的请求。 */
    public record UpdatePrivacySettingsDto(boolean canBeAddedByStrangers, boolean showProfileToStrangers, boolean muteMessagesFromUnknown, Boolean showFollowLists) {}
    public record UpdateProfileRequest(
        @Size(max = 2048) String avatarUrl,
        @NotBlank @Size(max = 40) String nickname,
        @Size(max = 160) String signature,
        @Min(0) @Max(2) Integer gender,
        @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 yyyy-MM-dd") String birthday,
        @Size(max = 32) String regionCode,
        @Pattern(regexp = "^$|1\\d{10}$", message = "手机号格式不正确") String phoneNumber,
        @Email @Size(max = 254) String email,
        @Size(max = 20) String pokeText,
        @Size(max = 80) String ringtoneName,
        UpdatePrivacySettingsDto privacySettings
    ) {}
    public record ProfileDto(String userId, String avatarUrl, String nickname, String signature, Integer gender, String birthday, String regionCode, String phoneNumber, String email, String pokeText, String ringtoneName, PrivacySettingsDto privacySettings) {
        static ProfileDto from(User u) { return new ProfileDto(u.getId().toString(),u.getAvatarUrl(),u.getNickname(),u.getSignature(),u.getGender(),u.getBirthday(),u.getRegionCode(),u.getPhoneNumber(),u.getEmail(),u.getPokeText(),u.getRingtoneName(),new PrivacySettingsDto(u.isCanBeAddedByStrangers(),u.isShowProfileToStrangers(),u.isMuteMessagesFromUnknown(),u.isShowFollowLists())); }
    }
}
