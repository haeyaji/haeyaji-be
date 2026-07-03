package com.haeyaji.be.weather.application;

import com.haeyaji.be.weather.application.port.in.WeatherQuery;
import com.haeyaji.be.weather.application.port.out.NowcastProvider;
import com.haeyaji.be.weather.application.port.out.UltraForecastProvider;
import com.haeyaji.be.weather.domain.HourlyWeather;
import com.haeyaji.be.weather.domain.LiveObservation;
import com.haeyaji.be.weather.domain.UltraForecastSlot;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 오늘 날씨 초단기(실황·예보) 오버레이 병합/폴백 검증 (이슈 #6).
 */
class WeatherServiceLiveOverlayTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final Clock fixedClock = Clock.fixed(
            ZonedDateTime.of(2026, 7, 3, 14, 0, 0, 0, ZONE).toInstant(), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 3);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    /** 단기예보 스텁: 맑음 26℃, 시간별 2칸. */
    private Weather shortTermSunny() {
        return Weather.builder()
                .cond(WeatherCondition.SUNNY).condKo("맑음")
                .temp(26).hi(30).lo(22).feels(27).pop(10)
                .humidity(50).windMs(2.0)
                .hourly(List.of(
                        new HourlyWeather("14:00", 26, 10),
                        new HourlyWeather("15:00", 27, 10)))
                .build();
    }

    private WeatherService service(NowcastProvider nowcast, UltraForecastProvider ultra) {
        return new WeatherService(
                (lat, lng, date) -> shortTermSunny(),
                (lat, lng, date) -> shortTermSunny(),
                nowcast, ultra,
                (lat, lng, date) -> null,
                (lat, lng) -> com.haeyaji.be.weather.domain.AirQuality.EMPTY,
                30, fixedClock);
    }

    @Test
    void 실황이_소나기면_단기예보가_맑음이어도_즉시_rainy() {
        NowcastProvider shower = (lat, lng) -> new LiveObservation(24.6, 85, 1.5, 4);
        Weather w = service(shower, (lat, lng) -> List.of())
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.RAINY);
        assertThat(w.condKo()).isEqualTo("소나기");
        assertThat(w.temp()).isEqualTo(25); // 24.6 실측 반올림 (예보 26 대체)
        assertThat(w.humidity()).isEqualTo(85);
    }

    @Test
    void 실황_강수없음이면_초단기예보_첫_슬롯의_하늘상태를_쓴다() {
        NowcastProvider clear = (lat, lng) -> new LiveObservation(26.0, 50, 2.0, 0);
        UltraForecastProvider cloudy = (lat, lng) -> List.of(
                new UltraForecastSlot("14:00", 27, 4, 0)); // 흐림
        Weather w = service(clear, cloudy).getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    void 초단기예보로_hourly_앞부분_기온이_갱신된다() {
        UltraForecastProvider ultra = (lat, lng) -> List.of(
                new UltraForecastSlot("14:00", 24, 1, 0),  // 26 → 24
                new UltraForecastSlot("16:00", 23, 1, 0)); // hourly에 없는 시각 → 무시
        Weather w = service((lat, lng) -> null, ultra)
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.hourly().get(0).temp()).isEqualTo(24);
        assertThat(w.hourly().get(1).temp()).isEqualTo(27); // 15:00 은 단기값 유지
        assertThat(w.hourly().get(0).pop()).isEqualTo(10);  // pop 은 초단기 미제공 → 유지
    }

    @Test
    void 실측이_예보_최고최저를_벗어나면_보정한다() {
        NowcastProvider hot = (lat, lng) -> new LiveObservation(31.4, 50, 2.0, 0);
        Weather w = service(hot, (lat, lng) -> List.of())
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.hi()).isEqualTo(31); // 30 → 31 보정
        assertThat(w.lo()).isEqualTo(22);
    }

    @Test
    void 초단기_전부_실패해도_단기예보_값이_그대로_유지된다() {
        Weather w = service((lat, lng) -> null, (lat, lng) -> List.of())
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.SUNNY);
        assertThat(w.temp()).isEqualTo(26);
        assertThat(w.feels()).isEqualTo(27);
    }

    @Test
    void 미래_날짜에는_오버레이를_적용하지_않는다() {
        NowcastProvider shower = (lat, lng) -> new LiveObservation(24.6, 85, 1.5, 4);
        Weather w = service(shower, (lat, lng) -> List.of())
                .getWeather(new WeatherQuery(37.5, 127.0, TOMORROW));

        assertThat(w.cond()).isEqualTo(WeatherCondition.SUNNY); // 실황 무시
        assertThat(w.temp()).isEqualTo(26);
    }
}
