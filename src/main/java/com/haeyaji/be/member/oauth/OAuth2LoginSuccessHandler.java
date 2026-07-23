package com.haeyaji.be.member.oauth;

import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.domain.MemberRole;
import com.haeyaji.be.member.jwt.JwtTokenProvider;
import com.haeyaji.be.member.jwt.RefreshTokenRepository;
import com.haeyaji.be.member.oauth.oauth2.CustomOAuth2User;
import com.haeyaji.be.member.oauth.oidc.CustomOidcUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend.callback-url}")
    private String frontendCallbackUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        Member member;
        boolean isNewMember;  // 첫 로그인 사용자는 온보딩 등 할 수 있도록 추가

        if (principal instanceof CustomOidcUser oidcUser) {
            member = oidcUser.getMember();
            isNewMember = oidcUser.isNewMember();
        } else if (principal instanceof CustomOAuth2User oauth2User) {
            member = oauth2User.getMember();
            isNewMember = oauth2User.isNewMember();
        } else {
            throw new IllegalStateException("알 수 없는 principal 타입: " + principal.getClass());
        }

        UUID memberId = member.getId();
        MemberRole memberRole = member.getRole();

        // access/refresh 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(memberId, memberRole);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        refreshTokenRepository.save(memberId, refreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));

        // 클라이언트에 토큰 전달 방식: 리다이렉트+쿼리파라미터 / ** httpOnly 쿠키 ** / JSON
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                // .secure(true) // https 환경 전제. 로컬 http 테스트땐 false로 낮춰야 동작함
                .secure(false)
                .path("/")  // accessToken은 모든 요청에서 확인해야 함
                .maxAge(Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityMs()))
                .sameSite("Lax")    // Todo: SameSite: Strict / Lax / None이 무엇인가?
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                // .secure(true) // https 환경 전제. 로컬 http 테스트땐 false로 낮춰야 동작함
                .secure(false)
                .path("/api/auth")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()))
                .sameSite("Lax")    // Todo: SameSite: Strict / Lax / None이 무엇인가?
                .build();

        String targetUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("isNewMember", isNewMember)
                .build().toUriString();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        response.sendRedirect(targetUrl);

    }
}
