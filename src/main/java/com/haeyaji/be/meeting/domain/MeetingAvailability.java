package com.haeyaji.be.meeting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 슬롯별 가능 인원 집계 (MEET-6·7). FREE만 카운트하며 BUSY·미응답은 0으로 본다.
 */
public record MeetingAvailability(
        List<SlotAvailability> slots,
        int participantCount
) {

    public static MeetingAvailability of(List<MeetingSlot> slots, List<SlotResponse> responses, int participantCount) {
        Map<UUID, Long> freeCounts = responses.stream()
                .filter(response -> response.status() == ResponseStatus.FREE)
                .collect(Collectors.groupingBy(SlotResponse::slotId, Collectors.counting()));
        List<SlotAvailability> cells = slots.stream()
                .sorted(Comparator.comparing(MeetingSlot::slotStartAt))
                .map(slot -> new SlotAvailability(
                        slot.id(),
                        slot.slotStartAt(),
                        freeCounts.getOrDefault(slot.id(), 0L).intValue()))
                .toList();
        return new MeetingAvailability(cells, participantCount);
    }

    public int maxFreeCount() {
        return slots.stream().mapToInt(SlotAvailability::freeCount).max().orElse(0);
    }

    /**
     * 가능 인원이 최대인 슬롯들의 최대 연속 구간 목록 (MEET-7).
     * 인접 판정은 슬롯 단위 간격이며, 최대 인원이 0이면 빈 목록.
     */
    public List<TimeWindow> bestWindows(int slotUnitMinutes) {
        int max = maxFreeCount();
        if (max == 0) {
            return List.of();
        }
        List<TimeWindow> windows = new ArrayList<>();
        LocalDateTime runStart = null;
        LocalDateTime previousStart = null;
        for (SlotAvailability slot : slots) {
            boolean isBest = slot.freeCount() == max;
            boolean continuesRun = runStart != null
                    && isBest
                    && slot.slotStartAt().equals(previousStart.plusMinutes(slotUnitMinutes));
            if (continuesRun) {
                previousStart = slot.slotStartAt();
                continue;
            }
            if (runStart != null) {
                windows.add(new TimeWindow(runStart, previousStart.plusMinutes(slotUnitMinutes), max));
                runStart = null;
            }
            if (isBest) {
                runStart = slot.slotStartAt();
                previousStart = slot.slotStartAt();
            }
        }
        if (runStart != null) {
            windows.add(new TimeWindow(runStart, previousStart.plusMinutes(slotUnitMinutes), max));
        }
        return windows;
    }
}
