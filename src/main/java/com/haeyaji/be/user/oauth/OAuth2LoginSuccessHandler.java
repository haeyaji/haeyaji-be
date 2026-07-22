package com.haeyaji.be.user.oauth;

import com.haeyaji.be.user.domain.User;
import com.haeyaji.be.user.domain.UserRole;
import com.haeyaji.be.user.jwt.JwtTokenProvider;
import com.haeyaji.be.user.oauth.oauth2.CustomOAuth2User;
import com.haeyaji.be.user.oauth.oidc.CustomOidcUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        User user;
        if (principal instanceof CustomOidcUser oidcUser) {
            user = oidcUser.getUser();
        } else if (principal instanceof CustomOAuth2User oauth2User) {
            user = oauth2User.getUser();
        } else {
            throw new IllegalStateException("알 수 없는 principal 타입: " + principal.getClass());
        }

        Long userId = user.getId();
        UserRole userRole = user.getRole();

        // access/refresh 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(userId, userRole);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        // Todo: refreshTokenRepository에 refresh token 저장 (TTL = jwt.refresh-token-validity-ms)

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

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    }
}
