package com.haeyaji.be.profile.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 설문 6분류(한글) → 카테고리 10종 코드 변환.
 * <p>여기가 뚫리면 한 필드에 두 어휘가 섞여 nlp 프롬프트로 그대로 흘러간다.
 */
class SurveyCategoryMapperTest {

    @Test
    void 설문_한_덩어리가_코드_여럿으로_풀린다() {
        // "야외"는 산책과 운동을 함께 뜻한다 — 하나만 고르면 나머지 취향을 통째로 잃는다.
        assertThat(SurveyCategoryMapper.normalize(List.of("야외")))
                .containsExactly("NATURE_WALK", "SPORTS_ACTIVITY");
        assertThat(SurveyCategoryMapper.normalize(List.of("맛집/카페")))
                .containsExactly("RESTAURANT", "CAFE_DESSERT");
    }

    @Test
    void 설문_선택지_여섯_개가_모두_변환된다() {
        // 하나라도 빠지면 그 항목을 고른 사용자는 선호가 비어버린다.
        List<String> all = List.of("야외", "실내", "휴식", "생산성", "사람만나기", "맛집/카페");
        for (String option : all) {
            assertThat(SurveyCategoryMapper.normalize(List.of(option)))
                    .as(option).isNotEmpty();
        }
    }

    @Test
    void 이미_코드로_들어오면_그대로_통과한다() {
        // fe가 나중에 코드로 바꿔 보내도 깨지지 않아야 한다.
        assertThat(SurveyCategoryMapper.normalize(List.of("CAFE_DESSERT", "SHOPPING")))
                .containsExactly("CAFE_DESSERT", "SHOPPING");
    }

    @Test
    void 알_수_없는_값은_버린다() {
        // 남겨두면 애초에 막으려던 어휘 혼재가 그대로 남는다.
        assertThat(SurveyCategoryMapper.normalize(Arrays.asList("야외", "듣도보도못한거", "", null)))
                .containsExactly("NATURE_WALK", "SPORTS_ACTIVITY");
    }

    @Test
    void 겹치는_선택은_한_번만_담고_고른_순서를_지킨다() {
        // "실내"와 "휴식"을 함께 고르면 코드가 겹치지 않지만, 코드와 라벨을 섞어 보내면 겹칠 수 있다.
        assertThat(SurveyCategoryMapper.normalize(List.of("맛집/카페", "CAFE_DESSERT", "휴식")))
                .containsExactly("RESTAURANT", "CAFE_DESSERT", "REST_HEALING");
    }

    @Test
    void 비었거나_없으면_빈_목록이다() {
        assertThat(SurveyCategoryMapper.normalize(null)).isEmpty();
        assertThat(SurveyCategoryMapper.normalize(List.of())).isEmpty();
    }

    @Test
    void 변환_결과는_모두_실제_카테고리_코드다() {
        // 오타로 없는 코드 이름을 적어두면 학습 쪽에서 조용히 무시된다.
        List<String> converted = SurveyCategoryMapper.normalize(
                List.of("야외", "실내", "휴식", "생산성", "사람만나기", "맛집/카페"));
        assertThat(converted).allSatisfy(name ->
                assertThat(Category.tryParse(name)).as(name).isPresent());
    }
}
