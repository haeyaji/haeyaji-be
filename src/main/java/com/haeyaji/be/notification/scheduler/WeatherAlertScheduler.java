package com.haeyaji.be.notification.scheduler;

import com.haeyaji.be.notification.domain.NotificationCategory;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.service.NotificationService;
import com.haeyaji.be.todo.domain.TodoStatus;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import com.haeyaji.be.weather.domain.Weather;
import com.haeyaji.be.weather.domain.WeatherCondition;
import com.haeyaji.be.weather.service.WeatherQuery;
import com.haeyaji.be.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 오늘 밖에서 할 일이 있는데 비·눈이 오면 아침에 미리 알린다 — 나가기 전에 알아야 우산을 챙기거나
 * 일정을 옮길 수 있으므로, 시작 직전이 아니라 하루 시작에 한 번 보낸다.
 *
 * <p>대상은 <b>좌표가 붙은 할 일</b>이다. 좌표가 있어야 그 자리 날씨를 볼 수 있고, 장소를 지정했다는 건
 * 이동을 전제한 일정이라는 뜻이기도 하다.
 *
 * <p>중복은 {@code notification} 유니크 제약 {@code (member_id, type, ref_id)}이 막는다
 * (TODO_WEATHER_ALERT는 멱등 대상) — 재기동으로 배치가 두 번 돌아도 알림은 하나다.
 * 좌표별 날씨 조회 비용은 날씨 캐시(Redis)가 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAlertScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final TodoRepository todoRepository;
    private final WeatherService weatherService;
    private final NotificationService notificationService;
    private final Clock clock;

    // 주기를 설정으로 뺀 이유: 배치는 하루 한 번만 도는 탓에 손으로 확인할 방법이 없다.
    @Scheduled(cron = "${haeyaji.notification.weather-alert-cron:0 0 7 * * *}", zone = "Asia/Seoul")
    public void alertBadWeather() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<TodoEntity> outdoor = todoRepository
                .findByTodoDateAndStatusAndLatIsNotNullAndLngIsNotNull(today, TodoStatus.TODO);
        for (TodoEntity todo : outdoor) {
            try {
                alertIfBad(todo, today);
            } catch (Exception e) {
                // 좌표 하나가 조회에 실패해도 나머지 일정은 알려야 한다.
                log.error("날씨 알림 실패: todoId={}", todo.getId(), e);
            }
        }
    }

    private void alertIfBad(TodoEntity todo, LocalDate today) {
        Weather weather = weatherService.getWeather(
                new WeatherQuery(todo.getLat(), todo.getLng(), today));
        if (weather == null || !isBad(weather.cond())) {
            return;
        }
        String body = "%s 예보예요. %s 일정을 확인해 보세요.".formatted(
                weather.condKo() == null ? label(weather.cond()) : weather.condKo(),
                todo.getPlaceName() == null ? "오늘" : todo.getPlaceName());
        notificationService.sendSystem(todo.getMemberId(), NotificationCategory.TODO,
                NotificationType.TODO_WEATHER_ALERT, todo.getTitle(), body, todo.getId(), null);
    }

    /** 우산·일정 변경을 부를 만한 날씨만 알린다 — 흐림까지 알리면 알림이 무뎌진다. */
    private static boolean isBad(WeatherCondition cond) {
        return cond == WeatherCondition.RAINY || cond == WeatherCondition.SNOWY;
    }

    private static String label(WeatherCondition cond) {
        return cond == WeatherCondition.SNOWY ? "눈" : "비";
    }
}
