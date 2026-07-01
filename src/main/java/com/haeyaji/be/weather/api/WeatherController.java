package com.haeyaji.be.weather.api;

import com.haeyaji.be.weather.api.dto.WeatherResponse;
import com.haeyaji.be.weather.application.port.in.GetWeatherUseCase;
import com.haeyaji.be.weather.application.port.in.WeatherQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 날씨 API 중계 (FR-2). fe는 be만 호출하고, 키·CORS·격자변환은 be가 처리한다.
 *
 * <pre>GET /api/weather?lat={위도}&lng={경도}&date={yyyy-MM-dd}</pre>
 * (context-path 가 /api 이므로 매핑은 /weather)
 */
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final GetWeatherUseCase getWeatherUseCase;

    @GetMapping
    public WeatherResponse getWeather(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return WeatherResponse.from(getWeatherUseCase.getWeather(new WeatherQuery(lat, lng, date)));
    }
}
