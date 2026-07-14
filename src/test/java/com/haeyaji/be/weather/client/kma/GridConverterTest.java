package com.haeyaji.be.weather.client.kma;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GridConverterTest {

    @Test
    void 서울시청_좌표는_격자_60_127() {
        GridConverter.Grid grid = GridConverter.toGrid(37.5665, 126.9780);
        assertThat(grid.nx()).isEqualTo(60);
        assertThat(grid.ny()).isEqualTo(127);
    }

    @Test
    void 부산_좌표는_격자_98_76() {
        GridConverter.Grid grid = GridConverter.toGrid(35.1796, 129.0756);
        assertThat(grid.nx()).isEqualTo(98);
        assertThat(grid.ny()).isEqualTo(76);
    }
}
