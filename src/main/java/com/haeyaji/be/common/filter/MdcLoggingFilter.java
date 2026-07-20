package com.haeyaji.be.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청의 컨텍스트(requestId, method, uri, clientIp)를 MDC 에 적재하는 필터.
 *
 * - 예외 핸들러를 포함한 "모든" 로그 라인에 요청 식별 정보가 자동으로 붙는다.
 * - X-Request-Id 헤더가 들어오면 그대로 사용(게이트웨이/LB 전파), 없으면 생성.
 * - 응답 헤더로 requestId 를 돌려주므로 클라이언트 문의 시 로그 추적이 가능하다.
 * - Micrometer Tracing(+ Zipkin/Tempo) 도입 시 traceId/spanId 는 자동으로
 *   MDC 에 들어오므로 이 필터와 공존한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[a-zA-Z0-9\\-_.]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = resolveRequestId(request);

        try {
            MDC.put(REQUEST_ID, requestId);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());
            MDC.put("clientIp", resolveClientIp(request));

            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // 스레드 풀 재사용에 의한 컨텍스트 누수 방지 — 반드시 clear
        }
    }

    /**
     * 상류(게이트웨이)에서 온 ID 는 재사용하되, 로그 인젝션 방지를 위해 형식 검증.
     */
    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // 최초 클라이언트 IP
        }
        return request.getRemoteAddr();
    }
}