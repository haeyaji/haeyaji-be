package com.haeyaji.be.weather.service;

import com.haeyaji.be.weather.client.airquality.AirKoreaClient;
import com.haeyaji.be.weather.client.kma.KmaMidTermWeatherClient;
import com.haeyaji.be.weather.client.kma.KmaNowcastClient;
import com.haeyaji.be.weather.client.kma.KmaUltraForecastClient;
import com.haeyaji.be.weather.client.kma.KmaWeatherClient;
import com.haeyaji.be.weather.client.livingidx.KmaUvClient;
import com.haeyaji.be.weather.domain.AirQuality;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final Clock fixedClock = Clock.fixed(
            ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZONE).toInstant(), ZONE);

    private final AtomicInteger shortCalls = new AtomicInteger();
    private final AtomicInteger midCalls = new AtomicInteger();

    private KmaWeatherClient shortProvider() {
        KmaWeatherClient mock = mock(KmaWeatherClient.class);
        when(mock.fetch(anyDouble(), anyDouble(), any())).thenAnswer(inv -> {
            shortCalls.incrementAndGet();
            return Weather.builder()
                    .cond(WeatherCondition.SUNNY).condKo("맑음")
                    .temp(26).hi(27).lo(19).feels(27).pop(5)
                    .humidity(45).windMs(3.2).build();
        });
        return mock;
    }

    private KmaMidTermWeatherClient midProvider() {
        KmaMidTermWeatherClient mock = mock(KmaMidTermWeatherClient.class);
        when(mock.fetch(anyDouble(), anyDouble(), any())).thenAnswer(inv -> {
            midCalls.incrementAndGet();
            return Weather.builder()
                    .cond(WeatherCondition.CLOUDY).condKo("흐림")
                    .temp(24).hi(28).lo(20).pop(30).build();
        });
        return mock;
    }

    private KmaUvClient uvProvider() {
        KmaUvClient mock = mock(KmaUvClient.class);
        when(mock.getUvIndex(anyDouble(), anyDouble(), any())).thenReturn(7);
        return mock;
    }

    private AirKoreaClient airProvider() {
        AirKoreaClient mock = mock(AirKoreaClient.class);
        when(mock.getAirQuality(anyDouble(), anyDouble())).thenReturn(new AirQuality(42, 18));
        return mock;
    }

    private KmaNowcastClient nowcastProvider() {
        KmaNowcastClient mock = mock(KmaNowcastClient.class);
        when(mock.getNowcast(anyDouble(), anyDouble())).thenReturn(null);
        return mock;
    }

    private KmaUltraForecastClient ultraProvider() {
        KmaUltraForecastClient mock = mock(KmaUltraForecastClient.class);
        when(mock.getUltraForecast(anyDouble(), anyDouble())).thenReturn(java.util.List.of());
        return mock;
    }

    private WeatherService service() {
        // 초단기(실황/예보)는 이 테스트 관심사가 아니므로 미제공(fail-soft 경로) 스텁
        return new WeatherService(shortProvider(), midProvider(),
                nowcastProvider(), ultraProvider(),
                uvProvider(), airProvider(), 30, fixedClock);
    }

    @Test
    void 오늘_plus3일까지는_단기예보를_쓴다() {
        service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 4))); // +3
        assertThat(shortCalls.get()).isEqualTo(1);
        assertThat(midCalls.get()).isZero();
    }

    @Test
    void plus4일부터는_중기예보를_쓴다() {
        service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 5))); // +4
        assertThat(midCalls.get()).isEqualTo(1);
        assertThat(shortCalls.get()).isZero();
    }

    @Test
    void 같은_좌표_날짜_재호출은_캐시로_상위API를_다시_부르지_않는다() {
        WeatherService service = service();
        WeatherQuery q = new WeatherQuery(37.5665, 126.9780, LocalDate.of(2026, 7, 1));
        service.getWeather(q);
        service.getWeather(q);
        assertThat(shortCalls.get()).isEqualTo(1);
    }

    @Test
    void plus10일까지는_중기예보를_쓴다() {
        service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 11))); // +10
        assertThat(midCalls.get()).isEqualTo(1);
    }

    @Test
    void 예보_범위_밖_미래날짜는_오늘로_대체된다() {
        // +11일은 범위(+10) 밖 → 오늘(+0)로 정규화 → 단기예보 호출
        service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 12)));
        assertThat(shortCalls.get()).isEqualTo(1);
        assertThat(midCalls.get()).isZero();
    }

    @Test
    void date가_null이면_오늘로_조회한다() {
        service().getWeather(new WeatherQuery(37.5, 127.0, null));
        assertThat(shortCalls.get()).isEqualTo(1);
    }

    @Test
    void 오늘_조회는_UV와_미세먼지를_보강한다() {
        Weather w = service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 1)));
        assertThat(w.uvIndex()).isEqualTo(7);
        assertThat(w.pm10()).isEqualTo(42);
        assertThat(w.pm25()).isEqualTo(18);
    }

    @Test
    void 미래날짜는_미세먼지를_채우지_않는다() {
        // 미세먼지는 실시간 관측이라 오늘만. UV는 예보라 미래도 가능.
        Weather w = service().getWeather(new WeatherQuery(37.5, 127.0, LocalDate.of(2026, 7, 3)));
        assertThat(w.pm10()).isNull();
        assertThat(w.pm25()).isNull();
        assertThat(w.uvIndex()).isEqualTo(7);
    }
}
