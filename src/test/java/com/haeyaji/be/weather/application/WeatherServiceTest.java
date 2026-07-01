package com.haeyaji.be.weather.application;

import com.haeyaji.be.weather.application.port.in.WeatherQuery;
import com.haeyaji.be.weather.application.port.out.AirQualityProvider;
import com.haeyaji.be.weather.application.port.out.MidTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.ShortTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.UvIndexProvider;
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

class WeatherServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final Clock fixedClock = Clock.fixed(
            ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, ZONE).toInstant(), ZONE);

    private final AtomicInteger shortCalls = new AtomicInteger();
    private final AtomicInteger midCalls = new AtomicInteger();

    private ShortTermWeatherProvider shortProvider() {
        return (lat, lng, date) -> {
            shortCalls.incrementAndGet();
            return Weather.builder()
                    .cond(WeatherCondition.SUNNY).condKo("맑음")
                    .temp(26).hi(27).lo(19).feels(27).pop(5)
                    .humidity(45).windMs(3.2).build();
        };
    }

    private MidTermWeatherProvider midProvider() {
        return (lat, lng, date) -> {
            midCalls.incrementAndGet();
            return Weather.builder()
                    .cond(WeatherCondition.CLOUDY).condKo("흐림")
                    .temp(24).hi(28).lo(20).pop(30).build();
        };
    }

    private UvIndexProvider uvProvider() {
        return (lat, lng, date) -> 7;
    }

    private AirQualityProvider airProvider() {
        return (lat, lng) -> new AirQuality(42, 18);
    }

    private WeatherService service() {
        return new WeatherService(shortProvider(), midProvider(), uvProvider(), airProvider(), 30, fixedClock);
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
