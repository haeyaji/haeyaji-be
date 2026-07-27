package com.haeyaji.be.profile.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 온보딩 설문의 <b>거친 6분류</b>(한글)를 학습·추천이 쓰는 {@link Category} 10종 코드로 옮긴다.
 *
 * <p>설문은 "야외 / 실내 / 휴식 / 생산성 / 사람만나기 / 맛집·카페"처럼 고르기 쉬운 큰 덩어리로 묻는데,
 * 학습(`member_category_weight`)과 nlp 프롬프트는 10종 코드를 쓴다. 그대로 저장하면 한 필드에
 * 두 어휘가 섞여, 신규 유저는 {@code "야외"}가 가고 학습이 쌓인 유저는 {@code NATURE_WALK}가 간다.
 *
 * <p>덩어리 하나가 코드 여럿에 대응하므로 <b>1:N</b>이다 — "야외"를 고르면 산책과 운동 둘 다 선호로 본다.
 * 반대로 {@code SHOPPING}은 설문에 대응 항목이 없어 행동 학습으로만 쌓인다.
 *
 * <p>이미 코드로 들어온 값은 그대로 통과시킨다(fe가 나중에 코드로 바꿔 보내도 깨지지 않게).
 * 어느 쪽도 아닌 값은 버린다 — 알 수 없는 어휘를 남겨두면 애초에 막으려던 혼재가 그대로 남는다.
 */
public final class SurveyCategoryMapper {

    private static final Map<String, List<Category>> BY_SURVEY_LABEL = Map.of(
            "야외", List.of(Category.NATURE_WALK, Category.SPORTS_ACTIVITY),
            "실내", List.of(Category.INDOOR_PLAY, Category.CULTURE_EXHIBIT),
            "휴식", List.of(Category.REST_HEALING),
            "생산성", List.of(Category.STUDY_WORK),
            "사람만나기", List.of(Category.SOCIAL),
            "맛집/카페", List.of(Category.RESTAURANT, Category.CAFE_DESSERT));

    private SurveyCategoryMapper() {
    }

    /**
     * 설문 응답을 코드 목록으로 정규화한다. 순서는 받은 순서를 지키고 중복은 없앤다.
     *
     * @param rawValues 설문 라벨(한글) 또는 이미 변환된 코드가 섞여 들어올 수 있다
     * @return 유효한 {@link Category} 코드 이름 목록. 알 수 없는 값은 빠진다
     */
    public static List<String> normalize(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String value = raw.strip();
            Category.tryParse(value).ifPresent(code -> result.add(code.name()));
            BY_SURVEY_LABEL.getOrDefault(value, List.of())
                    .forEach(code -> result.add(code.name()));
        }
        return List.copyOf(result);
    }
}
