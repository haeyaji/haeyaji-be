package com.haeyaji.be.weather.application;

import com.haeyaji.be.weather.application.port.in.GetWeatherUseCase;
import com.haeyaji.be.weather.application.port.in.WeatherQuery;
import com.haeyaji.be.weather.application.port.out.AirQualityProvider;
import com.haeyaji.be.weather.application.port.out.MidTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.ShortTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.UvIndexProvider;
import com.haeyaji.be.weather.domain.AirQuality;
import com.haeyaji.be.weather.domain.Weather;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 날씨 유스케이스 구현.
 * <ul>
 *   <li>date 정규화: 미지정 → 오늘, 예보 범위(오늘~+10일) 밖 → 오늘로 대체</li>
 *   <li>날짜별 소스 라우팅: 오늘~+3일은 단기예보(시간별), +4~+10일은 중기예보(일 단위)</li>
 *   <li>동일 좌표·날짜 재호출 캐싱(TTL)으로 상위 API 호출 절감</li>
 * </ul>
 */
@Slf4j
@Service
public class WeatherService implements GetWeatherUseCase {

    /** 단기예보로 처리하는 최대 미래일(오늘 + N일). */
    private static final int SHORT_MAX_AHEAD_DAYS = 3;
    /** 중기예보로 처리하는 최대 미래일(오늘 + N일). 중기예보는 +10일까지 제공. */
    private static final int MID_MAX_AHEAD_DAYS = 10;

    private final ShortTermWeatherProvider shortTermProvider;
    private final MidTermWeatherProvider midTermProvider;
    private final UvIndexProvider uvIndexProvider;
    private final AirQualityProvider airQualityProvider;
    private final Duration cacheTtl;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public WeatherService(ShortTermWeatherProvider shortTermProvider,
                          MidTermWeatherProvider midTermProvider,
                          UvIndexProvider uvIndexProvider,
                          AirQualityProvider airQualityProvider,
                          @Value("${haeyaji.weather.kma.cache-ttl-minutes:30}") long cacheTtlMinutes) {
        this(shortTermProvider, midTermProvider, uvIndexProvider, airQualityProvider,
                cacheTtlMinutes, Clock.systemDefaultZone());
    }

    WeatherService(ShortTermWeatherProvider shortTermProvider,
                   MidTermWeatherProvider midTermProvider,
                   UvIndexProvider uvIndexProvider,
                   AirQualityProvider airQualityProvider,
                   long cacheTtlMinutes, Clock clock) {
        this.shortTermProvider = shortTermProvider;
        this.midTermProvider = midTermProvider;
        this.uvIndexProvider = uvIndexProvider;
        this.airQualityProvider = airQualityProvider;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
        this.clock = clock;
    }

    @Override
    public Weather getWeather(WeatherQuery query) {
        LocalDate date = normalizeDate(query.date());
        String key = cacheKey(query.lat(), query.lng(), date);

        CacheEntry cached = cache.get(key);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.weather();
        }

        Weather weather = enrich(fetch(query.lat(), query.lng(), date), query.lat(), query.lng(), date);
        cache.put(key, new CacheEntry(weather, now.plus(cacheTtl)));
        return weather;
    }

    private Weather fetch(double lat, double lng, LocalDate date) {
        long offset = ChronoUnit.DAYS.between(LocalDate.now(clock), date);
        if (offset <= SHORT_MAX_AHEAD_DAYS) {
            return shortTermProvider.fetch(lat, lng, date);
        }
        return midTermProvider.fetch(lat, lng, date);
    }

    /**
     * 자외선지수·미세먼지를 보강한다. 두 소스는 별개(생활기상지수·에어코리아)이고 보조 정보이므로
     * 실패해도 null로 두고 날씨 본체는 유지한다(provider가 fail-soft).
     * 미세먼지는 실시간 관측이라 오늘 조회에만 채운다.
     */
    private Weather enrich(Weather weather, double lat, double lng, LocalDate date) {
        Integer uv = uvIndexProvider.getUvIndex(lat, lng, date);
        AirQuality air = date.isEqual(LocalDate.now(clock))
                ? airQualityProvider.getAirQuality(lat, lng)
                : AirQuality.EMPTY;
        return weather.withAirQuality(uv, air.pm10(), air.pm25());
    }

    private LocalDate normalizeDate(LocalDate requested) {
        LocalDate today = LocalDate.now(clock);
        if (requested == null) {
            return today;
        }
        if (requested.isBefore(today) || requested.isAfter(today.plusDays(MID_MAX_AHEAD_DAYS))) {
            // 예보 범위(오늘~+7일) 밖 → 오늘 값으로 대체 (fe 계약)
            return today;
        }
        return requested;
    }

    private String cacheKey(double lat, double lng, LocalDate date) {
        // 좌표는 소수점 3자리(~100m)로 반올림해 인접 재호출을 같은 키로 묶는다.
        return "%.3f:%.3f:%s".formatted(lat, lng, date);
    }

    private record CacheEntry(Weather weather, Instant expiresAt) {
    }
}
