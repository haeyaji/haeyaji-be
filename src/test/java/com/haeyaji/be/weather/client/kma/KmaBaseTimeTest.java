package com.haeyaji.be.weather.client.kma;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KmaBaseTimeTest {

    @Test
    void 발표_직후_10분_이내면_직전_발표시각을_쓴다() {
        // 14:05 → 14시 발표는 아직 미반영 → 11시 발표
        KmaBaseTime.BaseTime base = KmaBaseTime.resolve(LocalDateTime.of(2026, 7, 1, 14, 5));
        assertThat(base.baseDate()).isEqualTo("20260701");
        assertThat(base.baseTime()).isEqualTo("1100");
    }

    @Test
    void 발표_10분_경과하면_해당_발표시각을_쓴다() {
        KmaBaseTime.BaseTime base = KmaBaseTime.resolve(LocalDateTime.of(2026, 7, 1, 14, 15));
        assertThat(base.baseTime()).isEqualTo("1400");
    }

    @Test
    void 새벽_02시_발표_이전이면_전날_2300() {
        KmaBaseTime.BaseTime base = KmaBaseTime.resolve(LocalDateTime.of(2026, 7, 1, 1, 30));
        assertThat(base.baseDate()).isEqualTo("20260630");
        assertThat(base.baseTime()).isEqualTo("2300");
    }
}
