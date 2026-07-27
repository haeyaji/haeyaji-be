package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.meeting.domain.MeetingDeletedEvent;
import com.haeyaji.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 약속 삭제 → 그 약속을 가리키던 알림 정리 (MEET-16).
 *
 * <p>초대·확정·리마인더 알림은 {@code ref_id}·{@code link_token}으로 약속을 가리킨다.
 * 약속이 사라지면 눌러도 404가 나는 알림만 남으므로 함께 지운다.
 *
 * <p>같은 트랜잭션에서 돈다 — 삭제가 롤백되면 알림도 그대로 남아야 한다.
 * 여기서 지우면 <b>같은 약속을 다시 만들 때</b> 멱등 제약 {@code (member_id, type, ref_id)}도
 * 깨끗한 상태로 시작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingDeletedNotificationListener {

    private final NotificationRepository notificationRepository;

    @EventListener
    public void removeNotifications(MeetingDeletedEvent event) {
        long removed = notificationRepository.deleteByRefId(event.meetingId());
        if (removed > 0) {
            log.info("약속 삭제 → 알림 정리: meetingId={} 알림={}건", event.meetingId(), removed);
        }
    }
}
