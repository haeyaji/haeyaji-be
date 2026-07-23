package com.haeyaji.be.profile.service;

import com.haeyaji.be.profile.domain.Category;
import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;
import com.haeyaji.be.profile.domain.Signal;
import com.haeyaji.be.profile.repository.MemberCategoryWeightRepository;
import com.haeyaji.be.profile.repository.MemberKeywordWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 행동 가중치 학습. 신호를 맥락(날씨·시간대)별로 원자적으로 누적(UPSERT)하고, 주1회 decay한다.
 * <p>맥락 시각은 주입된 {@link Clock}(JVM 기본 = Asia/Seoul) 기준. 날씨는 fe feedback에 없어
 * MVP에선 {@link CtxWeather#CLEAR} 기본값(날씨별 세분화는 데이터 축적 후).
 * <p>누적은 {@code INSERT … ON DUPLICATE KEY UPDATE}로 원자 처리 — 동시 최초삽입 경합의 PK 위반(500)과
 * read-modify-write의 lost-update를 둘 다 제거한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightService {

    private static final CtxWeather DEFAULT_WEATHER = CtxWeather.CLEAR;
    private static final int KEYWORD_MAX_LEN = 50;

    private final MemberCategoryWeightRepository categoryWeightRepository;
    private final MemberKeywordWeightRepository keywordWeightRepository;
    private final Clock clock;

    /** 카테고리 신호 원자적 누적. 없으면 delta로 생성, 있으면 += delta. */
    @Transactional
    public void applyCategorySignal(UUID memberId, Category category, Signal signal) {
        CtxTimeOfDay timeOfDay = CtxTimeOfDay.from(LocalTime.now(clock));
        categoryWeightRepository.upsertWeight(
                toBytes(memberId), DEFAULT_WEATHER.name(), timeOfDay.name(),
                category.name(), signal.getDelta());
    }

    /** 키워드 신호 원자적 누적. nlp 자유 검색어 기반 — 아직 게이트웨이에서 미연결(향후). 공백 스킵·길이 상한. */
    @Transactional
    public void applyKeywordSignal(UUID memberId, String keyword, Signal signal) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String normalized = keyword.strip();
        if (normalized.length() > KEYWORD_MAX_LEN) {
            normalized = normalized.substring(0, KEYWORD_MAX_LEN);
        }
        keywordWeightRepository.upsertWeight(toBytes(memberId), normalized, signal.getDelta());
    }

    /** 주1회 decay — 두 테이블 x0.9. 배치에서 호출. */
    @Transactional
    public void decayAll() {
        int categories = categoryWeightRepository.decayAll();
        int keywords = keywordWeightRepository.decayAll();
        log.info("가중치 decay 적용: category={} keyword={}", categories, keywords);
    }

    /** UUID → binary(16) big-endian(msb→lsb). Hibernate의 UUID↔binary 저장 순서와 일치해 네이티브 바인딩에서 동일 row를 가리킨다. */
    private static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
