package com.haeyaji.be.weather.infrastructure.kma;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KmaUltraBaseTimeTest {

    @Test
    void 실황은_정시_발표_10분_후부터_해당_시각을_쓴다() {
        // 14:05 → 14시 발표 미반영 → 13시
        assertThat(KmaUltraBaseTime.nowcast(LocalDateTime.of(2026, 7, 3, 14, 5)).baseTime())
                .isEqualTo("1300");
        // 14:15 → 14시 발표 반영
        assertThat(KmaUltraBaseTime.nowcast(LocalDateTime.of(2026, 7, 3, 14, 15)).baseTime())
                .isEqualTo("1400");
    }

    @Test
    void 실황_자정_직후는_전날_23시() {
        KmaUltraBaseTime.BaseTime base = KmaUltraBaseTime.nowcast(LocalDateTime.of(2026, 7, 3, 0, 5));
        assertThat(base.baseDate()).isEqualTo("20260702");
        assertThat(base.baseTime()).isEqualTo("2300");
    }

    @Test
    void 예보는_매시_30분_발표_45분_후부터_해당_시각을_쓴다() {
        // 14:40 → 14:30 발표 미반영 → 13:30
        assertThat(KmaUltraBaseTime.forecast(LocalDateTime.of(2026, 7, 3, 14, 40)).baseTime())
                .isEqualTo("1330");
        // 14:50 → 14:30 발표 반영
        assertThat(KmaUltraBaseTime.forecast(LocalDateTime.of(2026, 7, 3, 14, 50)).baseTime())
                .isEqualTo("1430");
    }
}
