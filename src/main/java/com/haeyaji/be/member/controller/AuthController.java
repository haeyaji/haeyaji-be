package com.haeyaji.be.member.controller;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.domain.MemberRole;
import com.haeyaji.be.member.jwt.JwtTokenProvider;
import com.haeyaji.be.member.jwt.RefreshTokenRepository;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * 토큰 재발급 / 로그아웃 API.
 *
 * context-path가 이미 /api라서 이 컨트롤러의 실제 경로는 /api/auth/** 가 됨
 * (OAuth2LoginSuccessHandler에서 refreshToken 쿠키 path를 "/api/auth"로 잡아둔 것과 일치해야 함).
 *
 * refreshToken은 httpOnly 쿠키로만 오가므로 요청 바디로 받지 않는다.
 * (httpOnly라 프론트 JS가 애초에 값을 읽을 수 없어서 바디에 실어 보낼 수도 없음)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(HttpServletRequest request, HttpServletResponse response) {

        // 1) request에서 refreshToken 추출 (없으면 401)
        String refreshToken = resolveRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2) jwtTokenProvider.validateToken(refreshToken) 검증 (만료/위조면 401)
        if (!(jwtTokenProvider.validateToken(refreshToken))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 3) jwtTokenProvider.getMemberId(refreshToken)로 memberId 추출
        UUID memberId = jwtTokenProvider.getMemberId(refreshToken);

        // 4) refreshTokenRepository.findByMemberId(memberId)로 Redis에 저장된 값과 대조
        // 없거나 불일치하면 401 — 이미 로그아웃됐거나 탈취/재사용된 토큰일 수 있음
        String existingRefreshToken = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!refreshToken.equals(existingRefreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 5) memberRepository.findById(memberId)로 최신 role 조회
        // (refresh token엔 role 클레임이 없음 — 발급 이후 권한이 바뀌었을 수 있어서 새로 조회)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MemberRole role = member.getRole();

        // 6) jwtTokenProvider로 새 accessToken + refreshToken 발급
        String accessToken = jwtTokenProvider.createAccessToken(memberId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);    // Refresh Token Rotation (RTR)

        // 7) refreshTokenRepository.save()로 새 refreshToken을 Redis에 회전 저장 (재사용 공격 방지)
        // redisTemplate: 같은 key로 save하면 덮어쓰기됨
        refreshTokenRepository.save(memberId, newRefreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));

        // 8) 새 토큰들을 accessToken/refreshToken 쿠키로 다시 세팅
        // (OAuth2LoginSuccessHandler와 쿠키 옵션이 중복되니, 공용 헬퍼로 뽑는 것도 고려해볼 것)
        ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityMs()))
                .sameSite("Lax")
                .build();

        ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(false)      // Todo: 실제 운영에서는 모든 secure를 꼭 true로 바꿀 것 (Ctrl+Shift+F로 찾아보기)
                .path("/api/auth")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) {

        // 1) 리프레시 토큰 삭제
        refreshTokenRepository.deleteByMemberId(userDetails.getMemberId());

        // 2) 2개의 쿠키 삭제
        ResponseCookie expiredAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")  // name, domain, path 3개가 꼭 일치해야 같은 쿠키로 취급
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();

        ResponseCookie expiredRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")  // name, domain, path 3개가 꼭 일치해야 같은 쿠키로 취급
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString());

        return ResponseEntity.ok().build();
    }

    private String resolveRefreshTokenFromCookie(HttpServletRequest request) {

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
