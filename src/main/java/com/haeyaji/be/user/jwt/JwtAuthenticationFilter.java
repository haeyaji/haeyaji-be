package com.haeyaji.be.user.jwt;

import com.haeyaji.be.user.domain.UserRole;
import com.haeyaji.be.user.oauth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        // token 없으면 인증 없이 다음 필터로 넘김 (permitAll 경로면 이걸로 충분, 보호된 경로면 뒤에서 AuthorizationFilter가 401/403 처리)
        // return 필수 — 없으면 filterChain.doFilter()가 중복 호출됨
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Todo: (최적화) validateToken + getUserId + getRole 3번 발생하는 토큰 파싱 1번으로 줄이기
        if (jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);
            UserRole role = jwtTokenProvider.getRole(token);

            CustomUserDetails userDetails = new CustomUserDetails(userId, role);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 요청 보낸 IP, 세션ID등의 부가정보(details)를 담아준다
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        }

        filterChain.doFilter(request, response);
    }

    // "Bearer {token}" 형태에서 순수 토큰 문자열만 추출
    private String resolveToken(HttpServletRequest request) {

        // access token을 헤더에 담는 방식
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 문자열(7글자)을 제외한 순수 토큰 부분만 추출하여 반환
            return bearerToken.substring(7);
        }

        // access token을 쿠키에 담는 방식
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
