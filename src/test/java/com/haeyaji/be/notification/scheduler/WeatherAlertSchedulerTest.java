package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.mail.ReminderMailer;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import com.haeyaji.be.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 궂은 날씨 알림 — 알릴 날씨를 가리는 기준과, 한 건이 실패해도 나머지가 도는지.
 */
class WeatherAlertSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 27);

    private TodoRepository todoRepository;
    private WeatherService weatherService;
    private NotificationService notificationService;
    private WeatherAlertScheduler scheduler;

    private final UUID owner = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        weatherService = mock(WeatherService.class);
        notificationService = mock(NotificationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T22:00:00Z"), KST); // KST 07:00
        scheduler = new WeatherAlertScheduler(todoRepository, weatherService, notificationService,
                mock(ReminderMailer.class), clock);
    }

    private TodoEntity outdoorTodo(String title) {
        return TodoEntity.create(owner, title, TODAY, LocalTime.of(15, 0),
                "한강공원", null, 37.5, 127.0, null, TodoSource.MANUAL, false, 0);
    }

    private Weather weather(WeatherCondition cond, String condKo) {
        return new Weather(cond, condKo, 25, 28, 20, 26, 80, 60, 2.0, 3, 20, 10, List.of());
    }

    @Test
    void 비_예보면_알린다() {
        when(todoRepository.findByTodoDateAndStatusAndLatIsNotNullAndLngIsNotNull(eq(TODAY), any()))
                .thenReturn(List.of(outdoorTodo("한강 산책")));
        when(weatherService.getWeather(any())).thenReturn(weather(WeatherCondition.RAINY, "소나기"));

        scheduler.alertBadWeather();

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendSystem(eq(owner), any(),
                eq(NotificationType.TODO_WEATHER_ALERT), eq("한강 산책"), body.capture(), any(), eq(null));
        assertThat(body.getValue()).contains("소나기").contains("한강공원");
    }

    @Test
    void 맑거나_흐리면_알리지_않는다() {
        // 흐림까지 알리면 알림이 무뎌져 정작 비 오는 날을 놓친다.
        when(todoRepository.findByTodoDateAndStatusAndLatIsNotNullAndLngIsNotNull(any(), any()))
                .thenReturn(List.of(outdoorTodo("산책")));
        when(weatherService.getWeather(any())).thenReturn(weather(WeatherCondition.CLOUDY, "흐림"));

        scheduler.alertBadWeather();

        verify(notificationService, never()).sendSystem(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 좌표_한_건의_조회_실패가_나머지_일정을_막지_않는다() {
        TodoEntity broken = outdoorTodo("실패할 일정");
        TodoEntity fine = outdoorTodo("정상 일정");
        when(todoRepository.findByTodoDateAndStatusAndLatIsNotNullAndLngIsNotNull(any(), any()))
                .thenReturn(List.of(broken, fine));
        when(weatherService.getWeather(any()))
                .thenThrow(new RuntimeException("기상청 응답 없음"))
                .thenReturn(weather(WeatherCondition.SNOWY, "눈"));

        scheduler.alertBadWeather();

        verify(notificationService).sendSystem(any(), any(), any(), eq("정상 일정"), any(), any(), any());
    }
}
