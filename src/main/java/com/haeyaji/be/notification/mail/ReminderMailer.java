package com.haeyaji.be.notification.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 다가온 일정·궂은 날씨를 메일로 알린다 (템플릿 {@code haeyaji-todo-reminder}).
 *
 * <p>두 알림이 같은 템플릿을 쓰는 이유: 받는 사람 입장에선 둘 다 "곧 있을 일정 안내"고,
 * 다른 점은 날씨 문구 유무뿐이다. 시각·장소·날씨 줄은 값이 없으면 템플릿에서 통째로 빠진다.
 *
 * <p><b>알림이 실제로 만들어졌을 때만</b> 호출해야 한다 — 스케줄러는 5분마다 같은 일정을 다시 집는데,
 * 중복은 알림 저장의 유니크 제약이 걸러낸다. 그 결과를 보지 않고 보내면 메일만 5분마다 나간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderMailer {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final MemberMails memberMails;
    private final NotificationMailSender mailSender;
    private final MailTemplates mailTemplates;
    private final MailLinks mailLinks;

    /**
     * @param startTime   시작 시각. 없으면 메일에서 "시작" 줄이 빠진다.
     * @param placeName   장소. 없으면 "장소" 줄이 빠진다.
     * @param weatherLine 날씨 안내 문장. 없으면 날씨 상자가 빠진다.
     */
    public void send(UUID memberId, String todoTitle, LocalTime startTime,
                     String placeName, String weatherLine) {
        String email = memberMails.of(memberId);
        if (email == null) {
            return; // 이메일을 안 준 회원 — 알림함으로는 이미 전달된다
        }
        Map<String, String> values = new HashMap<>();
        values.put("todoTitle", todoTitle);
        values.put("startTime", startTime == null ? "" : startTime.format(HH_MM));
        values.put("placeName", placeName == null ? "" : placeName);
        values.put("weatherLine", weatherLine == null ? "" : weatherLine);
        values.put("link", mailLinks.app());

        mailSender.send(email, "[해야지] '%s' 곧 시작해요".formatted(todoTitle),
                mailTemplates.render("haeyaji-todo-reminder", values));
    }
}
