package com.haeyaji.be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf.disable())

                .formLogin(login -> login.disable())

                .httpBasic(basic -> basic.disable())




                ;


        return http.build();

    }
}
