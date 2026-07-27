package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.meeting.domain.MeetingStatus;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 확정된 약속 시작 10분 전, 참여자 전원(생성자 포함 — 생성 시점에 본인도 참여자 행으로 저장됨)에게
 * MEETING_REMINDER를 발송한다. actor가 없는 시스템 알림이라 sendSystem()을 쓴다.
 * meeting/todo 도메인 서비스는 notification을 모르게 하는 이벤트 리스너 컨벤션과 반대로, 스케줄러는
 * 걸어들 이벤트가 없어(도메인 액션이 없음) notification 쪽이 meeting 리포지터리를 직접 참조한다 —
 * MeetingEventListener가 닉네임 조회용으로 MemberRepository를 참조하는 것과 같은 방향의 의존.
 * 매분 실행되며, "지금 + 10분"을 분 단위로 truncate한 시각과 confirmedStartAt이 정확히 일치하는
 * 약속만 대상으로 삼는다(범위(BETWEEN) 대신 등치 비교라 자정 경계에서도 안전).
 * NotificationType.MEETING_REMINDER는 IDEMPOTENT_TYPES에 포함되어 (memberId, meetingId) 조합당
 * 한 번만 저장/발송되므로, 재배포나 실행 지연으로 같은 분이 두 번 스캔돼도 중복 알림은 가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingReminderScheduler {

    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void remindUpcomingMeetings() {
        LocalDateTime targetStartAt = LocalDateTime.now(clock.withZone(SCHEDULE_ZONE))
                .truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(10);

        List<MeetingEntity> dueMeetings =
                meetingRepository.findByStatusAndConfirmedStartAt(MeetingStatus.CONFIRMED, targetStartAt);

        for (MeetingEntity meeting : dueMeetings) {
            List<MeetingParticipantEntity> participants =
                    meetingParticipantRepository.findByMeetingIdOrderByJoinedAt(meeting.getId());
            String body = "약속이 10분 후 시작됩니다: " + meeting.getConfirmedStartAt();
            for (MeetingParticipantEntity participant : participants) {
                try {
                    notificationService.sendSystem(
                            participant.getMemberId(),
                            NotificationCategory.INVITE, NotificationType.MEETING_REMINDER,
                            meeting.getTitle(), body, meeting.getId()
                    );
                } catch (Exception e) {
                    log.error("MEETING_REMINDER 알림 발송 실패: meetingId={}, memberId={}",
                            meeting.getId(), participant.getMemberId(), e);
                }
            }
        }
    }
}
