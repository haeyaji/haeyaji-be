package com.haeyaji.be.weather.application;

import com.haeyaji.be.weather.application.port.in.GetWeatherUseCase;
import com.haeyaji.be.weather.application.port.in.WeatherQuery;
import com.haeyaji.be.weather.application.port.out.AirQualityProvider;
import com.haeyaji.be.weather.application.port.out.MidTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.NowcastProvider;
import com.haeyaji.be.weather.application.port.out.ShortTermWeatherProvider;
import com.haeyaji.be.weather.application.port.out.UltraForecastProvider;
import com.haeyaji.be.weather.application.port.out.UvIndexProvider;
import com.haeyaji.be.weather.domain.AirQuality;
import com.haeyaji.be.weather.domain.FeelsLike;
import com.haeyaji.be.weather.domain.HourlyWeather;
import com.haeyaji.be.weather.domain.LiveObservation;
import com.haeyaji.be.weather.domain.UltraForecastSlot;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 날씨 유스케이스 구현.
 * <ul>
 *   <li>date 정규화: 미지정 → 오늘, 예보 범위(오늘~+10일) 밖 → 오늘로 대체</li>
 *   <li>날짜별 소스 라우팅: 오늘~+3일은 단기예보(시간별), +4~+10일은 중기예보(일 단위)</li>
 *   <li>오늘 조회는 초단기실황(관측)·초단기예보(+6h)로 덮어써 돌발 강수를 빠르게 반영</li>
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
    /** 오늘 조회 캐시 TTL(분). 초단기 반영을 위해 기본 TTL보다 짧게. */
    private static final long TODAY_CACHE_TTL_MINUTES = 10;

    private final ShortTermWeatherProvider shortTermProvider;
    private final MidTermWeatherProvider midTermProvider;
    private final NowcastProvider nowcastProvider;
    private final UltraForecastProvider ultraForecastProvider;
    private final UvIndexProvider uvIndexProvider;
    private final AirQualityProvider airQualityProvider;
    private final Duration cacheTtl;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public WeatherService(ShortTermWeatherProvider shortTermProvider,
                          MidTermWeatherProvider midTermProvider,
                          NowcastProvider nowcastProvider,
                          UltraForecastProvider ultraForecastProvider,
                          UvIndexProvider uvIndexProvider,
                          AirQualityProvider airQualityProvider,
                          @Value("${haeyaji.weather.kma.cache-ttl-minutes:30}") long cacheTtlMinutes) {
        this(shortTermProvider, midTermProvider, nowcastProvider, ultraForecastProvider,
                uvIndexProvider, airQualityProvider, cacheTtlMinutes, Clock.systemDefaultZone());
    }

    WeatherService(ShortTermWeatherProvider shortTermProvider,
                   MidTermWeatherProvider midTermProvider,
                   NowcastProvider nowcastProvider,
                   UltraForecastProvider ultraForecastProvider,
                   UvIndexProvider uvIndexProvider,
                   AirQualityProvider airQualityProvider,
                   long cacheTtlMinutes, Clock clock) {
        this.shortTermProvider = shortTermProvider;
        this.midTermProvider = midTermProvider;
        this.nowcastProvider = nowcastProvider;
        this.ultraForecastProvider = ultraForecastProvider;
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

        boolean today = date.isEqual(LocalDate.now(clock));
        Weather weather = fetch(query.lat(), query.lng(), date);
        if (today) {
            weather = applyLiveOverlay(weather, query.lat(), query.lng());
        }
        weather = enrich(weather, query.lat(), query.lng(), date);

        // 오늘은 초단기(매시 갱신) 반영을 위해 짧은 TTL 사용
        Duration ttl = today ? Duration.ofMinutes(TODAY_CACHE_TTL_MINUTES) : cacheTtl;
        cache.put(key, new CacheEntry(weather, now.plus(ttl)));
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
     * 오늘 날씨에 초단기실황(관측)·초단기예보(+6h)를 덮어쓴다.
     * <ul>
     *   <li>temp/humidity/windMs ← 실황 실측값, feels 재계산</li>
     *   <li>cond ← 실황 PTY 우선(돌발 강수 즉시 반영), 없으면 초단기예보 첫 슬롯의 SKY/PTY</li>
     *   <li>hourly 앞 6시간 temp ← 초단기예보 (pop은 초단기 미제공이라 단기값 유지)</li>
     *   <li>hi/lo ← 실측이 예보 범위를 벗어나면 보정</li>
     * </ul>
     * 두 소스 모두 fail-soft(null/빈 목록)라 실패 시 단기예보 값이 그대로 유지된다.
     */
    private Weather applyLiveOverlay(Weather w, double lat, double lng) {
        LiveObservation obs = nowcastProvider.getNowcast(lat, lng);
        List<UltraForecastSlot> ultra = ultraForecastProvider.getUltraForecast(lat, lng);
        if (obs == null && ultra.isEmpty()) {
            return w;
        }

        int temp = obs != null ? (int) Math.round(obs.temp()) : w.temp();
        Integer humidity = obs != null && obs.humidity() != null ? obs.humidity() : w.humidity();
        Double windMs = obs != null && obs.windMs() != null ? obs.windMs() : w.windMs();
        Integer feels = humidity != null && windMs != null
                ? FeelsLike.calculate(temp, humidity, windMs)
                : w.feels();

        WeatherCondition cond = w.cond();
        String condKo = w.condKo();
        if (obs != null && obs.pty() != 0) {
            // 실황이 강수를 관측 → 단기예보가 뭐라 했든 즉시 반영 (소나기 대응)
            cond = WeatherCondition.resolve(1, obs.pty(), temp);
            condKo = WeatherCondition.describeKo(1, obs.pty());
        } else if (!ultra.isEmpty()) {
            UltraForecastSlot first = ultra.get(0);
            if (first.sky() != null && first.pty() != null) {
                cond = WeatherCondition.resolve(first.sky(), first.pty(), temp);
                condKo = WeatherCondition.describeKo(first.sky(), first.pty());
            }
        }

        List<HourlyWeather> hourly = overlayHourly(w.hourly(), ultra);

        return Weather.builder()
                .cond(cond)
                .condKo(condKo)
                .temp(temp)
                .hi(Math.max(w.hi(), temp))
                .lo(Math.min(w.lo(), temp))
                .feels(feels)
                .pop(w.pop())
                .humidity(humidity)
                .windMs(windMs)
                .uvIndex(w.uvIndex())
                .pm10(w.pm10())
                .pm25(w.pm25())
                .hourly(hourly)
                .build();
    }

    /** hourly 중 초단기예보와 시각이 겹치는 칸의 기온을 최신값으로 교체. */
    private List<HourlyWeather> overlayHourly(List<HourlyWeather> hourly, List<UltraForecastSlot> ultra) {
        if (ultra.isEmpty()) {
            return hourly;
        }
        Map<String, Integer> ultraTemp = new ConcurrentHashMap<>();
        for (UltraForecastSlot slot : ultra) {
            if (slot.temp() != null) {
                ultraTemp.put(slot.time(), slot.temp());
            }
        }
        List<HourlyWeather> merged = new ArrayList<>(hourly.size());
        for (HourlyWeather h : hourly) {
            Integer t = ultraTemp.get(h.time());
            merged.add(t != null ? new HourlyWeather(h.time(), t, h.pop()) : h);
        }
        return merged;
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
            // 예보 범위(오늘~+10일) 밖 → 오늘 값으로 대체 (fe 계약)
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
