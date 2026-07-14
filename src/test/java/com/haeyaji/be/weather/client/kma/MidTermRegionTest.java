package com.haeyaji.be.weather.client.kma;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MidTermRegionTest {

    @Test
    void 서울_좌표는_서울_지역코드() {
        MidTermRegion r = MidTermRegion.nearest(37.5665, 126.9780);
        assertThat(r.landRegId()).isEqualTo("11B00000");
        assertThat(r.taRegId()).isEqualTo("11B10101");
    }

    @Test
    void 부산_좌표는_부산_지역코드() {
        MidTermRegion r = MidTermRegion.nearest(35.1796, 129.0756);
        assertThat(r.landRegId()).isEqualTo("11H20000");
        assertThat(r.taRegId()).isEqualTo("11H20201");
    }

    @Test
    void 제주_좌표는_제주_지역코드() {
        MidTermRegion r = MidTermRegion.nearest(33.4996, 126.5312);
        assertThat(r.landRegId()).isEqualTo("11G00000");
    }
}
