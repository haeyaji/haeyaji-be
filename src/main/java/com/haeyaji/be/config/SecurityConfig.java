package com.haeyaji.be.config;

import com.haeyaji.be.member.jwt.JwtAuthenticationFilter;
import com.haeyaji.be.member.jwt.JwtTokenProvider;
import com.haeyaji.be.member.oauth.OAuth2LoginSuccessHandler;
import com.haeyaji.be.member.oauth.error.JwtAccessDeniedHandler;
import com.haeyaji.be.member.oauth.error.JwtAuthenticationEntryPoint;
import com.haeyaji.be.member.oauth.oauth2.CustomOAuth2UserService;
import com.haeyaji.be.member.oauth.oidc.CustomOidcUserService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class)

                .cors(Customizer.withDefaults())

                .formLogin(login -> login.disable())

                .httpBasic(basic -> basic.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)   // naver
                                .oidcUserService(customOidcUserService) // kakao/google
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // SSE(알림 스트림)처럼 비동기로 처리하는 요청은 끝날 때 ASYNC 디스패치로 필터체인을 한 번 더 탄다.
                        // 이때 SecurityContext는 이미 비워져 있어 인증 검사를 다시 걸면 무조건 Access Denied가 나고,
                        // 응답은 이미 커밋된 뒤라 500 스택트레이스만 로그에 쌓인다. 최초 진입에서 이미 인증했으므로 재디스패치는 통과시킨다.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/auth/reissue", "/weather/**", "/places/**", "/message").permitAll()
                        .anyRequest().authenticated())

                ;


        return http.build();

    }
}
