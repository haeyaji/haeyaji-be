package com.haeyaji.be.meeting.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.meeting.domain.MeetingErrorCode;
import com.haeyaji.be.meeting.domain.MeetingStatus;
import com.haeyaji.be.meeting.repository.MeetingEntity;
import com.haeyaji.be.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 공유 토큰 조회와 쓰기 가드(확정·만료 차단)의 단일 창구. 서비스 간 중복을 막는다.
 */
@Component
@RequiredArgsConstructor
public class MeetingFinder {

    private final MeetingRepository meetingRepository;

    public MeetingEntity getByShareToken(String shareToken) {
        return meetingRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    /** 수집 중(COLLECTING)인 약속만 반환. 확정이면 409, 마감이 지났으면 410. 응답 제출·초대·합류용. */
    public MeetingEntity getCollecting(String shareToken, LocalDateTime now) {
        MeetingEntity entity = getUnconfirmed(shareToken);
        if (entity.toDomain().statusAt(now) == MeetingStatus.EXPIRED) {
            throw new BusinessException(MeetingErrorCode.MEETING_EXPIRED);
        }
        return entity;
    }

    /**
     * 아직 확정되지 않은 약속을 반환(마감이 지났어도 허용). <b>방장 확정</b> 전용.
     * <p>마감은 "응답 수집 종료"이지 "약속 폐기"가 아니다 — 마감 후 집계 결과를 보고 시간을 확정하는 게
     * 정상 흐름이라, 여기서 만료를 막으면 마감된 약속을 영영 확정할 수 없게 된다.
     */
    public MeetingEntity getUnconfirmed(String shareToken) {
        MeetingEntity entity = getByShareToken(shareToken);
        if (entity.getStatus() == MeetingStatus.CONFIRMED) {
            throw new BusinessException(MeetingErrorCode.MEETING_ALREADY_CONFIRMED);
        }
        return entity;
    }
}
