package com.haeyaji.be.member.controller;

import com.haeyaji.be.member.oauth.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MainController {

    // 로그인 테스트용
    @GetMapping("/")
    public String index() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().toString();

        return "[username (id)] " + username + "\n[role] " + role;
    }

    // JWT 인증 테스트용 — SecurityConfig의 permitAll 목록("/", "/login/**", "/oauth2/**", "/weather/**", "/places/**")에
    // 없어서 anyRequest().authenticated()에 걸림. accessToken 쿠키가 없거나 만료되면 JwtAuthenticationEntryPoint가 401을 반환하고,
    // 유효하면 JwtAuthenticationFilter가 채운 CustomUserDetails가 여기로 들어옴.
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return Map.of(
                "memberId", userDetails.getMemberId(),
                "role", userDetails.getAuthorities()
        );
    }
}
