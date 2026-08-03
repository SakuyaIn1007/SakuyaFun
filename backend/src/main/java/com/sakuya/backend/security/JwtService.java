package com.sakuya.backend.security;

import com.sakuya.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration:7d}") Duration expiration) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    public String create(User user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("account", user.getAccount())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plus(expiration))).signWith(key).compact();
    }
    public UUID parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
