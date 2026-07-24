package com.haeyaji.be.recommend.service;

import com.haeyaji.be.profile.domain.CtxTimeOfDay;
import com.haeyaji.be.profile.domain.CtxWeather;
import com.haeyaji.be.profile.repository.MemberCategoryWeightEntity;
import com.haeyaji.be.profile.repository.MemberCategoryWeightRepository;
import com.haeyaji.be.profile.repository.MemberKeywordWeightEntity;
import com.haeyaji.be.profile.repository.MemberKeywordWeightRepository;
import com.haeyaji.be.profile.repository.MemberPreferenceEntity;
import com.haeyaji.be.profile.repository.MemberPreferenceRepository;
import com.haeyaji.be.recommend.client.nlp.dto.NlpMessageRequest.UserProfile;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DB의 취향 신호(가중치)와 설문·최근 행동을 nlp가 소비할 {@link UserProfile} 텍스트로 증류한다.
 * <p>점수/임베딩은 계산하지 않는다 — nlp가 텍스트만 소비하므로 "무엇을 선호/회피하는지" 라벨만 뽑는다.
 * <p>맥락은 현재 시간대 + {@link CtxWeather#CLEAR}(신호가 CLEAR로만 쌓이는 MVP 저장과 일치). vibe는 fe mood로 덮어쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileDistillService {

    private static final int TOP_CATEGORIES = 3;
    private static final int TOP_KEYWORDS = 5;
    private static final int RECENT_TODOS = 5;
    private static final CtxWeather DEFAULT_WEATHER = CtxWeather.CLEAR;

    private final MemberCategoryWeightRepository categoryWeightRepository;
    private final MemberKeywordWeightRepository keywordWeightRepository;
    private final MemberPreferenceRepository preferenceRepository;
    private final TodoRepository todoRepository;
    private final Clock clock;

    /**
     * @param moodOverride fe가 보낸 mood(=vibe). 있으면 설문 vibe보다 우선.
     * @return 프로필이 하나도 없으면(콜드스타트) 빈 필드의 UserProfile — nlp는 프로필 없이도 동작.
     */
    public UserProfile buildUserProfile(UUID memberId, String moodOverride) {
        MemberPreferenceEntity pref = preferenceRepository.findById(memberId).orElse(null);

        List<String> preferredCategories = topCategories(memberId, pref);
        List<String> avoid = pref != null ? pref.getAvoid() : List.of();
        String vibe = StringUtils.hasText(moodOverride) ? moodOverride
                : (pref != null ? pref.getVibe() : null);
        String intensity = pref != null ? pref.getIntensity() : null;
        List<String> recentSelections = recentSelections(memberId);

        return new UserProfile(preferredCategories, vibe, intensity, avoid, recentSelections);
    }

    /**
     * 현재 맥락(시간대+CLEAR)에서 가중치 양수인 카테고리 top-K.
     * 맥락에 데이터가 없으면 맥락 무시 top-K, 그것도 없으면 설문 preferredCategories로 폴백.
     */
    private List<String> topCategories(UUID memberId, MemberPreferenceEntity pref) {
        CtxTimeOfDay timeOfDay = CtxTimeOfDay.from(LocalTime.now(clock));
        List<String> contextual = pickPositive(
                categoryWeightRepository.findByMemberIdAndCtxWeatherAndCtxTimeOfDayOrderByWeightDesc(
                        memberId, DEFAULT_WEATHER, timeOfDay));
        if (!contextual.isEmpty()) {
            return contextual;
        }
        List<String> global = pickPositive(
                categoryWeightRepository.findByMemberIdOrderByWeightDesc(memberId));
        if (!global.isEmpty()) {
            return global;
        }
        return pref != null && pref.getPreferredCategories() != null
                ? pref.getPreferredCategories() : List.of();
    }

    private List<String> pickPositive(List<MemberCategoryWeightEntity> weights) {
        List<String> result = new ArrayList<>();
        for (MemberCategoryWeightEntity w : weights) {
            if (w.getWeight() <= 0) {
                continue; // 무시(음수)·미학습(0)은 선호로 보내지 않음
            }
            String name = w.getCategory().name();
            if (!result.contains(name)) {
                result.add(name);
            }
            if (result.size() >= TOP_CATEGORIES) {
                break;
            }
        }
        return result;
    }

    /** 키워드 취향 top-K + 최근 AI로 담은 할 일의 장소/제목. 중복 제거하며 합친다. */
    private List<String> recentSelections(UUID memberId) {
        Set<String> selections = new LinkedHashSet<>();
        for (MemberKeywordWeightEntity kw : keywordWeightRepository.findByMemberIdOrderByWeightDesc(memberId)) {
            if (kw.getWeight() > 0) {
                selections.add(kw.getKeyword());
            }
            if (selections.size() >= TOP_KEYWORDS) {
                break;
            }
        }
        for (TodoEntity todo : todoRepository
                .findTop10ByMemberIdAndSourceOrderByCreatedAtDesc(memberId, TodoSource.AI)) {
            String label = StringUtils.hasText(todo.getPlaceName()) ? todo.getPlaceName() : todo.getTitle();
            if (StringUtils.hasText(label)) {
                selections.add(label);
            }
            if (selections.size() >= TOP_KEYWORDS + RECENT_TODOS) {
                break;
            }
        }
        return new ArrayList<>(selections);
    }
}
