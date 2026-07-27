package com.haeyaji.be.notification.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메일 본문 템플릿 로더. {@code resources/mail/*.html}을 읽어 <code>{{키}}</code>를 값으로 치환한다.
 *
 * <p>템플릿 원본·디자인 가이드는 {@code docs/email-templates.html} 참고(브랜드 톤·메일 호환 규칙).
 * 값은 <b>넣을 때 자동으로 escape</b>하므로 호출부에서 따로 처리할 필요가 없다 — 약속 제목 같은
 * 사용자 입력이 메일 HTML을 깨뜨리거나 링크를 위조하는 걸 막는다.
 *
 * <p>템플릿은 처음 읽을 때 캐시한다(파일 수는 적고 변하지 않는다).
 */
@Slf4j
@Component
public class MailTemplates {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * @param name 확장자를 뺀 템플릿 이름 (예: {@code meeting-invite})
     * @param values <code>{{키}}</code> → 값. 값은 escape되어 삽입된다.
     * @return 치환된 HTML. 템플릿을 못 읽으면 빈 문자열(메일은 부가 기능이라 예외를 던지지 않는다).
     */
    public String render(String name, Map<String, String> values) {
        String template = cache.computeIfAbsent(name, this::load);
        if (template.isEmpty()) {
            return "";
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escape(entry.getValue()));
        }
        return rendered;
    }

    private String load(String name) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("mail/" + name + ".html").getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("메일 템플릿을 읽지 못했습니다: {} ({})", name, e.toString());
            return "";
        }
    }

    /** 사용자 입력이 마크업으로 해석되지 않게 한다. 링크는 URL-safe 문자만 쓰므로 영향 없음. */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
