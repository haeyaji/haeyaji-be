package com.haeyaji.be.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CsrfCookieFilter extends OncePerRequestFilter {

    /**
         function getCookie(name) {
         return document.cookie.split('; ').find(row => row.startsWith(name + '='))?.split('=')[1];
         }

         fetch('/api/auth/logout', {
         method: 'POST',
         credentials: 'include',
         headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') }
         }).then(r => console.log(r.status));

        브라우저 콘솔에서 위 코드로 {POST} /api/auth/logout 테스트 시 403 뜨는 현상 해결
     */

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        csrfToken.getToken();  // 강제로 로드시켜서 쿠키가 실제로 응답에 실리게 함
        filterChain.doFilter(request, response);
    }
}