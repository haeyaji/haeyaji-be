package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.MeetingDeletedEvent;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingType;
import com.haeyaji.be.meeting.domain.TimeGrid;
import com.haeyaji.be.meeting.dto.MeetingUpdateRequest;
import com.haeyaji.be.meeting.repository.MeetingDateRepository;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingParticipantRepository;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import com.haeyaji.be.meeting.repository.MeetingResponseEntity;
import com.haeyaji.be.meeting.repository.MeetingResponseRepository;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotEntity;
import com.haeyaji.be.meeting.repository.MeetingTimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 약속 수정·삭제의 잠금 규칙 — 방장만, 응답이 들어오면 수정 잠금, 확정 후 수정 잠금.
 * <p>남이 골라둔 시간을 소리 없이 날리지 않는 것이 이 규칙의 목적이다.
 */
class MeetingServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String TOKEN = "tok";

    private MeetingRepository meetingRepository;
    private MeetingDateRepository meetingDateRepository;
    private MeetingTimeSlotRepository meetingTimeSlotRepository;
    private MeetingParticipantRepository meetingParticipantRepository;
    private MeetingResponseRepository meetingResponseRepository;
    private MeetingFinder meetingFinder;
    private ApplicationEventPublisher eventPublisher;
    private MeetingService service;

    private final UUID creator = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meetingRepository = mock(MeetingRepository.class);
        meetingDateRepository = mock(MeetingDateRepository.class);
        meetingTimeSlotRepository = mock(MeetingTimeSlotRepository.class);
        meetingParticipantRepository = mock(MeetingParticipantRepository.class);
        meetingResponseRepository = mock(MeetingResponseRepository.class);
        meetingFinder = mock(MeetingFinder.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T01:00:00Z"), KST);
        service = new MeetingService(meetingRepository, meetingDateRepository, meetingTimeSlotRepository,
                meetingParticipantRepository, meetingResponseRepository, meetingFinder, eventPublisher, clock);

        when(meetingDateRepository.findByMeetingIdOrderByCandidateDate(any())).thenReturn(List.of());
        when(meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(any())).thenReturn(List.of());
        when(meetingParticipantRepository.findByMeetingIdAndInviteStatusOrderByJoinedAt(any(), any()))
                .thenReturn(List.of());
        when(meetingResponseRepository.findByMeetingTimeSlotIdIn(any())).thenReturn(List.of());
    }

    private MeetingEntity meeting() {
        MeetingEntity entity = MeetingEntity.create(creator, "원래 제목", MeetingType.TEAM,
                TimeGrid.of(LocalTime.of(18, 0), LocalTime.of(21, 0), 60),
                LocalDateTime.of(2026, 7, 28, 12, 0), TOKEN);
        when(meetingFinder.getUnconfirmed(TOKEN)).thenReturn(entity);
        when(meetingFinder.getByShareToken(TOKEN)).thenReturn(entity);
        return entity;
    }

    private MeetingUpdateRequest titleOnly(String title) {
        return new MeetingUpdateRequest(title, null, null, null, null, null);
    }

    @Test
    void 방장은_제목을_바꿀_수_있다() {
        MeetingEntity entity = meeting();

        service.update(TOKEN, creator, titleOnly("바뀐 제목"));

        assertThat(entity.getTitle()).isEqualTo("바뀐 제목");
    }

    @Test
    void 방장이_아니면_수정할_수_없다() {
        meeting();

        assertThatThrownBy(() -> service.update(TOKEN, stranger, titleOnly("가로채기")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.NOT_MEETING_CREATOR);
    }

    @Test
    void 응답이_하나라도_있으면_수정이_잠긴다() {
        // 시간 칸을 다시 깔면 남이 골라둔 가능 시간이 통째로 사라지고, 고른 사람은 알 방법이 없다.
        meeting();
        MeetingTimeSlotEntity slot = mock(MeetingTimeSlotEntity.class);
        when(slot.getId()).thenReturn(UUID.randomUUID());
        when(meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(any())).thenReturn(List.of(slot));
        when(meetingResponseRepository.findByMeetingTimeSlotIdIn(any()))
                .thenReturn(List.of(mock(MeetingResponseEntity.class)));

        assertThatThrownBy(() -> service.update(TOKEN, creator, titleOnly("바꿔보기")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.MEETING_ALREADY_RESPONDED);
    }

    @Test
    void 확정된_약속은_수정할_수_없다() {
        // 확정 잠금은 조회 창구(MeetingFinder.getUnconfirmed)가 담당한다 — 여기선 그 신호가 전달되는지만 본다.
        when(meetingFinder.getUnconfirmed(TOKEN))
                .thenThrow(new BusinessException(MeetingErrorCode.MEETING_ALREADY_CONFIRMED));

        assertThatThrownBy(() -> service.update(TOKEN, creator, titleOnly("확정 후 수정")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.MEETING_ALREADY_CONFIRMED);
    }

    @Test
    void 시간_범위는_네_값을_다_보내야_바꿀_수_있다() {
        // 일부만 받으면 남은 값과 어긋난 격자가 만들어진다.
        meeting();
        MeetingUpdateRequest partial =
                new MeetingUpdateRequest(null, List.of(LocalDate.of(2026, 8, 1)), null, null, null, null);

        assertThatThrownBy(() -> service.update(TOKEN, creator, partial))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.INCOMPLETE_TIME_GRID);
        verify(meetingTimeSlotRepository, never()).deleteByMeetingId(any());
    }

    @Test
    void 시간_범위를_바꾸면_후보_날짜와_시간_칸을_다시_깐다() {
        MeetingEntity entity = meeting();
        MeetingUpdateRequest full = new MeetingUpdateRequest(null,
                List.of(LocalDate.of(2026, 8, 1)), LocalTime.of(10, 0), LocalTime.of(12, 0), 30, null);

        service.update(TOKEN, creator, full);

        verify(meetingDateRepository).deleteByMeetingId(entity.getId());
        verify(meetingTimeSlotRepository).deleteByMeetingId(entity.getId());
        assertThat(entity.getSlotUnitMinutes()).isEqualTo(30);
        assertThat(entity.getTimeStart()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void 방장이_아니면_삭제할_수_없다() {
        meeting();

        assertThatThrownBy(() -> service.delete(TOKEN, stranger))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", MeetingErrorCode.NOT_MEETING_CREATOR);
        verify(meetingRepository, never()).delete(any());
    }

    @Test
    void 삭제는_딸린_행을_치우고_바깥_정리를_이벤트로_알린다() {
        MeetingEntity entity = meeting();
        MeetingTimeSlotEntity slot = mock(MeetingTimeSlotEntity.class);
        UUID slotId = UUID.randomUUID();
        when(slot.getId()).thenReturn(slotId);
        when(meetingTimeSlotRepository.findByMeetingIdOrderBySlotStartAt(any())).thenReturn(List.of(slot));

        service.delete(TOKEN, creator);

        // 응답은 슬롯을 참조하므로 슬롯보다 먼저 지워야 한다.
        verify(meetingResponseRepository).deleteByMeetingTimeSlotIdIn(List.of(slotId));
        verify(meetingTimeSlotRepository).deleteByMeetingId(entity.getId());
        verify(meetingDateRepository).deleteByMeetingId(entity.getId());
        verify(meetingParticipantRepository).deleteByMeetingId(entity.getId());
        verify(meetingRepository).delete(entity);
        verify(eventPublisher).publishEvent(new MeetingDeletedEvent(entity.getId(), "원래 제목"));
    }
}
