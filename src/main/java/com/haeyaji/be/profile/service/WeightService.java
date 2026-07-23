package com.haeyaji.be.profile.service;

import com.haeyaji.be.profile.domain.Category;
import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;
import com.haeyaji.be.profile.domain.Signal;
import com.haeyaji.be.profile.repository.MemberCategoryWeightEntity;
import com.haeyaji.be.profile.repository.MemberCategoryWeightId;
import com.haeyaji.be.profile.repository.MemberCategoryWeightRepository;
import com.haeyaji.be.profile.repository.MemberKeywordWeightEntity;
import com.haeyaji.be.profile.repository.MemberKeywordWeightId;
import com.haeyaji.be.profile.repository.MemberKeywordWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 행동 가중치 학습. 신호를 맥락(날씨·시간대)별로 누적하고(UPSERT), 주1회 decay한다.
 * <p>맥락 시각은 주입된 {@link Clock}(JVM 기본 = Asia/Seoul) 기준. 날씨는 fe feedback에 없어
 * MVP에선 {@link CtxWeather#CLEAR} 기본값(날씨별 세분화는 데이터 축적 후).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightService {

    private static final CtxWeather DEFAULT_WEATHER = CtxWeather.CLEAR;

    private final MemberCategoryWeightRepository categoryWeightRepository;
    private final MemberKeywordWeightRepository keywordWeightRepository;
    private final Clock clock;

    /**
     * 카테고리 신호 누적(UPSERT). 없으면 0에서 생성 후 델타 적용.
     * <p>findById→save는 이론상 동시 최초삽입 경합이 있으나, 여기 결과물은 퍼지한 취향 가중치라
     * 드문 경합에서 증분 1건을 잃어도 무해하고 다음 신호에 자연 보정된다(로그인 500과 달리 사용자 영향 없음).
     */
    @Transactional
    public void applyCategorySignal(UUID memberId, Category category, Signal signal) {
        CtxTimeOfDay timeOfDay = CtxTimeOfDay.from(LocalTime.now(clock));
        MemberCategoryWeightId id =
                new MemberCategoryWeightId(memberId, DEFAULT_WEATHER, timeOfDay, category);
        MemberCategoryWeightEntity entity = categoryWeightRepository.findById(id)
                .orElseGet(() -> MemberCategoryWeightEntity.create(
                        memberId, DEFAULT_WEATHER, timeOfDay, category));
        entity.addDelta(signal.getDelta());
        categoryWeightRepository.save(entity);
    }

    /** 키워드 신호 누적(UPSERT). nlp 검색어 기반 — 커밋3 게이트웨이에서 주입. */
    @Transactional
    public void applyKeywordSignal(UUID memberId, String keyword, Signal signal) {
        MemberKeywordWeightId id = new MemberKeywordWeightId(memberId, keyword);
        MemberKeywordWeightEntity entity = keywordWeightRepository.findById(id)
                .orElseGet(() -> MemberKeywordWeightEntity.create(memberId, keyword));
        entity.addDelta(signal.getDelta());
        keywordWeightRepository.save(entity);
    }

    /** 주1회 decay — 두 테이블 x0.9. 배치에서 호출. */
    @Transactional
    public void decayAll() {
        int categories = categoryWeightRepository.decayAll();
        int keywords = keywordWeightRepository.decayAll();
        log.info("가중치 decay 적용: category={} keyword={}", categories, keywords);
    }
}
