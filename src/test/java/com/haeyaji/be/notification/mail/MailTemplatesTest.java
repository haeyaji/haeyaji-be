package com.haeyaji.be.notification.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메일 템플릿 렌더링 — 값이 빈 구간을 빼는지, 사용자 입력이 마크업으로 새지 않는지,
 * 그리고 실제 템플릿 4종에 미치환 변수가 남지 않는지.
 */
class MailTemplatesTest {

    /** 실제로 쓰는 템플릿과 그 변수 — 이름이 어긋나면 메일에 {{키}}가 그대로 찍힌다. */
    private static final Map<String, List<String>> CONTRACT = Map.of(
            "haeyaji-meeting-invite", List.of("inviterName", "link", "meetingTitle"),
            "haeyaji-meeting-confirmed", List.of("link", "meetingTitle", "when"),
            "haeyaji-todo-share", List.of("inviterName", "link", "roleLabel", "todoTitle"),
            "haeyaji-todo-reminder", List.of("link", "placeName", "startTime", "todoTitle", "weatherLine"));

    private MailTemplates templates;

    @BeforeEach
    void setUp() {
        templates = new MailTemplates();
    }

    private static Map<String, String> filled(List<String> keys, String value) {
        Map<String, String> values = new HashMap<>();
        keys.forEach(key -> values.put(key, value));
        return values;
    }

    @Test
    void 템플릿_4종에_치환되지_않은_변수가_남지_않는다() {
        CONTRACT.forEach((name, keys) -> {
            String html = templates.render(name, filled(keys, "값"));
            assertThat(html).as(name).isNotEmpty();
            assertThat(html).as(name + "에 미치환 변수").doesNotContain("{{");
        });
    }

    @Test
    void 값이_비면_그_구간이_통째로_빠진다() {
        // 장소·시각·날씨는 없을 수 있다 — 빈 칸이 남으면 메일이 고장 난 것처럼 보인다.
        Map<String, String> values = filled(CONTRACT.get("haeyaji-todo-reminder"), "값");
        values.put("placeName", "");
        values.put("weatherLine", "  ");

        String html = templates.render("haeyaji-todo-reminder", values);

        assertThat(html).doesNotContain("장소").doesNotContain("{{");
        assertThat(html).contains("시작"); // 값이 있는 구간은 남는다
    }

    @Test
    void 값이_있으면_구간_표시만_걷어내고_내용은_남긴다() {
        Map<String, String> values = filled(CONTRACT.get("haeyaji-todo-reminder"), "값");
        values.put("weatherLine", "소나기 예보예요.");

        String html = templates.render("haeyaji-todo-reminder", values);

        assertThat(html).contains("소나기 예보예요.");
        assertThat(html).doesNotContain("{{?").doesNotContain("{{/");
    }

    @Test
    void 사용자_입력은_마크업으로_해석되지_않는다() {
        // 할 일 제목은 사용자가 쓴 글이라 태그가 섞이면 메일 본문·링크가 위조될 수 있다.
        Map<String, String> values = filled(CONTRACT.get("haeyaji-todo-share"), "값");
        values.put("todoTitle", "<script>alert(1)</script>");

        String html = templates.render("haeyaji-todo-share", values);

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void 없는_템플릿은_빈_문자열이다() {
        // 메일은 부가 기능이라 템플릿이 없다고 본래 작업을 실패시키지 않는다.
        assertThat(templates.render("haeyaji-does-not-exist", Map.of())).isEmpty();
    }
}
