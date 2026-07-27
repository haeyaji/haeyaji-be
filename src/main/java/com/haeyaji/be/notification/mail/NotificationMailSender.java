package com.haeyaji.be.notification.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 알림 이메일 발송 (Resend HTTP API).
 *
 * <p>SMTP(587) 대신 HTTPS API를 쓴다 — 사내·학내 네트워크가 SMTP 포트를 막는 경우가 흔하고
 * (실제로 이 프로젝트 개발 환경에서 smtp.resend.com:587 연결이 막혔다), Resend 자체도 HTTP API가
 * 기본 경로다({@code re_...} 키가 그 용도).
 *
 * <p><b>부가 기능이라 절대 본 작업을 방해하지 않는다</b> — 어떤 실패(키 미설정·API 오류·주소 불량)에도
 * 예외를 밖으로 던지지 않고 로그만 남긴다. 약속 확정·초대 같은 본 트랜잭션이 메일 때문에 롤백되면 안 되기 때문.
 * 발송은 {@code @Async}로 빼서 응답 지연도 만들지 않는다.
 *
 * <p>{@code haeyaji.mail.enabled=false}이거나 수신자 이메일이 없으면 조용히 건너뛴다
 * (카카오 로그인은 이메일이 선택동의라 값이 없을 수 있다 → 온보딩에서 직접 입력받는다).
 */
@Slf4j
@Component
public class NotificationMailSender {

    private static final String SEND_PATH = "/emails";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final boolean enabled;
    private final String apiKey;
    private final String from;

    @Autowired
    public NotificationMailSender(@Value("${haeyaji.mail.base-url}") String baseUrl,
                                  @Value("${haeyaji.mail.enabled:true}") boolean enabled,
                                  @Value("${haeyaji.mail.api-key:}") String apiKey,
                                  @Value("${haeyaji.mail.from}") String from,
                                  @Value("${haeyaji.mail.from-name:해야지}") String fromName) {
        this(WebClient.builder().baseUrl(baseUrl).build(), enabled, apiKey,
                "%s <%s>".formatted(fromName, from));
    }

    /** 테스트용: WebClient 직접 주입. */
    NotificationMailSender(WebClient webClient, boolean enabled, String apiKey, String from) {
        this.webClient = webClient;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.from = from;
    }

    /** HTML 본문 메일 발송. 실패는 흡수한다(호출부는 결과를 신경 쓰지 않아도 된다). */
    @Async
    public void send(String to, String subject, String html) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.debug("메일 발송 비활성(enabled={}, key여부={}) — 건너뜀: to={}",
                    enabled, StringUtils.hasText(apiKey), to);
            return;
        }
        if (!StringUtils.hasText(to)) {
            log.debug("수신자 이메일이 없어 건너뜀: subject={}", subject);
            return;
        }
        try {
            webClient.post()
                    .uri(SEND_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("from", from, "to", List.of(to), "subject", subject, "html", html))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            log.info("알림 메일 발송: to={} subject={}", to, subject);
        } catch (Exception e) {
            // 메일은 부가 기능 — 실패해도 본 흐름(약속 확정 등)에 영향을 주지 않는다.
            log.warn("알림 메일 발송 실패: to={} subject={} err={}", to, subject, e.toString());
        }
    }
}
