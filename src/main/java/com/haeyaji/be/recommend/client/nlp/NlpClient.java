package com.haeyaji.be.recommend.client.nlp;

import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageRequest;
import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * nlp 추천 서비스 아웃바운드 어댑터. be가 조립한 프로필·스케줄 맥락을 실어 {@code POST /api/message}를 대행한다.
 * <p>nlp가 LLM을 호출하므로 응답이 느릴 수 있어 타임아웃을 넉넉히(20초) 준다.
 * 실패 시 {@code null}을 반환해 상위(게이트웨이)가 사용자향 오류로 변환하도록 한다(KakaoLocalClient와 동일한 에러 흡수).
 */
@Slf4j
@Component
public class NlpClient {

    private static final String MESSAGE_PATH = "/api/message";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final WebClient webClient;

    @Autowired
    public NlpClient(@Value("${haeyaji.nlp.base-url}") String baseUrl) {
        this(WebClient.builder().baseUrl(baseUrl).build());
    }

    /** 테스트용: WebClient 직접 주입. */
    NlpClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /** nlp 추천 호출. 실패(타임아웃·오류응답·연결불가) 시 null. */
    public NlpMessageResponse message(NlpMessageRequest request) {
        try {
            return webClient.post()
                    .uri(MESSAGE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(NlpMessageResponse.class)
                    .timeout(TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.warn("nlp message call failed: {}", e.toString());
            return null;
        }
    }
}
