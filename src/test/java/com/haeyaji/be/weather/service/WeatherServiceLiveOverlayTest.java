package com.haeyaji.be.weather.service;

import com.haeyaji.be.weather.client.airquality.AirKoreaClient;
import com.haeyaji.be.weather.client.kma.KmaMidTermWeatherClient;
import com.haeyaji.be.weather.client.kma.KmaNowcastClient;
import com.haeyaji.be.weather.client.kma.KmaUltraForecastClient;
import com.haeyaji.be.weather.client.kma.KmaWeatherClient;
import com.haeyaji.be.weather.client.livingidx.KmaUvClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private KmaNowcastClient nowcast(LiveObservation obs) {
        KmaNowcastClient mock = mock(KmaNowcastClient.class);
        when(mock.getNowcast(anyDouble(), anyDouble())).thenReturn(obs);
        return mock;
    }

    private KmaUltraForecastClient ultra(List<UltraForecastSlot> slots) {
        KmaUltraForecastClient mock = mock(KmaUltraForecastClient.class);
        when(mock.getUltraForecast(anyDouble(), anyDouble())).thenReturn(slots);
        return mock;
    }

    private WeatherService service(KmaNowcastClient nowcast, KmaUltraForecastClient ultra) {
        KmaWeatherClient shortTerm = mock(KmaWeatherClient.class);
        when(shortTerm.fetch(anyDouble(), anyDouble(), any())).thenAnswer(inv -> shortTermSunny());
        KmaMidTermWeatherClient midTerm = mock(KmaMidTermWeatherClient.class);
        when(midTerm.fetch(anyDouble(), anyDouble(), any())).thenAnswer(inv -> shortTermSunny());
        KmaUvClient uv = mock(KmaUvClient.class);
        when(uv.getUvIndex(anyDouble(), anyDouble(), any())).thenReturn(null);
        AirKoreaClient air = mock(AirKoreaClient.class);
        when(air.getAirQuality(anyDouble(), anyDouble()))
                .thenReturn(com.haeyaji.be.weather.domain.AirQuality.EMPTY);
        return new WeatherService(shortTerm, midTerm, nowcast, ultra, uv, air, 30, fixedClock);
    }

    @Test
    void 실황이_소나기면_단기예보가_맑음이어도_즉시_rainy() {
        Weather w = service(nowcast(new LiveObservation(24.6, 85, 1.5, 4)), ultra(List.of()))
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.RAINY);
        assertThat(w.condKo()).isEqualTo("소나기");
        assertThat(w.temp()).isEqualTo(25); // 24.6 실측 반올림 (예보 26 대체)
        assertThat(w.humidity()).isEqualTo(85);
    }

    @Test
    void 실황_강수없음이면_초단기예보_첫_슬롯의_하늘상태를_쓴다() {
        Weather w = service(nowcast(new LiveObservation(26.0, 50, 2.0, 0)),
                ultra(List.of(new UltraForecastSlot("14:00", 27, 4, 0)))) // 흐림
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.CLOUDY);
    }

    @Test
    void 초단기예보로_hourly_앞부분_기온이_갱신된다() {
        Weather w = service(nowcast(null), ultra(List.of(
                new UltraForecastSlot("14:00", 24, 1, 0),  // 26 → 24
                new UltraForecastSlot("16:00", 23, 1, 0)))) // hourly에 없는 시각 → 무시
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.hourly().get(0).temp()).isEqualTo(24);
        assertThat(w.hourly().get(1).temp()).isEqualTo(27); // 15:00 은 단기값 유지
        assertThat(w.hourly().get(0).pop()).isEqualTo(10);  // pop 은 초단기 미제공 → 유지
    }

    @Test
    void 실측이_예보_최고최저를_벗어나면_보정한다() {
        Weather w = service(nowcast(new LiveObservation(31.4, 50, 2.0, 0)), ultra(List.of()))
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.hi()).isEqualTo(31); // 30 → 31 보정
        assertThat(w.lo()).isEqualTo(22);
    }

    @Test
    void 초단기_전부_실패해도_단기예보_값이_그대로_유지된다() {
        Weather w = service(nowcast(null), ultra(List.of()))
                .getWeather(new WeatherQuery(37.5, 127.0, TODAY));

        assertThat(w.cond()).isEqualTo(WeatherCondition.SUNNY);
        assertThat(w.temp()).isEqualTo(26);
        assertThat(w.feels()).isEqualTo(27);
    }

    @Test
    void 미래_날짜에는_오버레이를_적용하지_않는다() {
        Weather w = service(nowcast(new LiveObservation(24.6, 85, 1.5, 4)), ultra(List.of()))
                .getWeather(new WeatherQuery(37.5, 127.0, TOMORROW));

        assertThat(w.cond()).isEqualTo(WeatherCondition.SUNNY); // 실황 무시
        assertThat(w.temp()).isEqualTo(26);
    }
}
