package com.haeyaji.be.config;

import com.haeyaji.be.user.jwt.JwtAuthenticationFilter;
import com.haeyaji.be.user.jwt.JwtTokenProvider;
import com.haeyaji.be.user.oauth.OAuth2LoginSuccessHandler;
import com.haeyaji.be.user.oauth.oauth2.CustomOAuth2UserService;
import com.haeyaji.be.user.oauth.oidc.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf.disable())

                .formLogin(login -> login.disable())

                .httpBasic(basic -> basic.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)   // naver
                                .oidcUserService(customOidcUserService) // kakao/google
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/weather/**", "/places/**").permitAll()
                        .anyRequest().authenticated())

                ;


        return http.build();

    }
}
