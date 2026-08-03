package com.sakuya.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakuya.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/auth/**", "/actuator/health", "/uploads/**", "/ws/**").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); res.setContentType(MediaType.APPLICATION_JSON_VALUE); res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                mapper.writeValue(res.getWriter(), ApiResponse.error(401, "登录已过期，请重新登录"));
            }))
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}
