package com.haeyaji.be.common.provider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HaeyajiTimeProvider implements DateTimeProvider {

    private final Clock systemDefaultClock;

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(
                LocalDateTime.now(systemDefaultClock)
        );
    }
}
