package com.sakuya.backend.auth;

import com.sakuya.backend.common.*;
import com.sakuya.backend.security.JwtService;
import com.sakuya.backend.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtService jwt;
    public AuthController(UserRepository users, PasswordEncoder passwords, JwtService jwt) { this.users = users; this.passwords = passwords; this.jwt = jwt; }
    @PostMapping("/login") ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByAccountIgnoreCase(request.account()).orElseThrow(() -> new BusinessException(401, "账号或密码错误"));
        if (!passwords.matches(request.password(), user.getPasswordHash())) throw new BusinessException(401, "账号或密码错误");
        return ApiResponse.ok(new AuthTokenResponse(jwt.create(user), user.getId().toString(), user.getNickname()));
    }
    @PostMapping("/register") @Transactional ApiResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByAccountIgnoreCase(request.account())) throw new BusinessException(400, "账号已存在");
        User user = users.save(new User(request.account().trim(), passwords.encode(request.password()), request.nickname().trim()));
        return ApiResponse.ok(new AuthTokenResponse(jwt.create(user), user.getId().toString(), user.getNickname()));
    }
    public record LoginRequest(@NotBlank String account, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank @Size(min=3,max=64) String account, @NotBlank @Size(min=6,max=72) String password, @NotBlank @Size(max=40) String nickname) {}
    public record AuthTokenResponse(String token, String userId, String nickname) {}
}
