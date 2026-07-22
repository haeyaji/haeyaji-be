package com.haeyaji.be.config;

import com.haeyaji.be.user.oauth.oauth2.CustomOAuth2UserService;
import com.haeyaji.be.user.oauth.oidc.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf.disable())

                .formLogin(login -> login.disable())

                .httpBasic(basic -> basic.disable())

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)   // naver
                                .oidcUserService(customOidcUserService) // kakao/google
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/weather/**", "/places/**").permitAll()
                        .anyRequest().authenticated())

                ;


        return http.build();

    }
}
