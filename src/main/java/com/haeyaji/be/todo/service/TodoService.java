package com.haeyaji.be.todo.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.label.repository.LabelRepository;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoUpdatedEvent;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoParticipantEntity;
import com.haeyaji.be.todo.repository.TodoParticipantRepository;
import com.haeyaji.be.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    /** 과거날짜 검증을 JVM 기본 타임존(컨테이너 UTC일 수 있음) 대신 KST로 고정한다(L4). */
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final TodoRepository todoRepository;
    private final TodoParticipantRepository todoParticipantRepository;
    private final LabelRepository labelRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /** 캘린더 정렬: 고정(pinned) 먼저 → sortOrder → 생성순. 내 소유·공유받은 것을 한 목록에 합쳐 정렬한다. */
    private static final Comparator<TodoView> DISPLAY_ORDER =
            Comparator.comparing((TodoView v) -> v.todo().pinned(), Comparator.reverseOrder())
                    .thenComparingInt(v -> v.todo().sortOrder())
                    .thenComparing(v -> v.todo().createdAt(), Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * 선택 날짜의 할 일 = 내가 소유한 것 + 내가 수락(ACCEPTED)한 공유 할 일 중 그 날짜 것.
     * 공유 할 일은 행이 1개(소유자 것)라 완료 상태도 공동으로 공유된다(공동 완료). {@code sharedRole}로 소유/공유·권한을 구분.
     */
    public List<TodoView> getTodosByDate(UUID memberId, LocalDate date) {
        List<TodoView> result = new ArrayList<>();
        todoRepository.findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(memberId, date)
                .forEach(e -> result.add(TodoView.owned(e.toDomain())));

        Map<UUID, ParticipantRole> roleByTodoId = todoParticipantRepository
                .findByMemberIdAndInviteStatus(memberId, InviteStatus.ACCEPTED).stream()
                .collect(Collectors.toMap(TodoParticipantEntity::getTodoId, TodoParticipantEntity::getRole, (a, b) -> a));
        if (!roleByTodoId.isEmpty()) {
            todoRepository.findByIdInAndTodoDate(roleByTodoId.keySet(), date)
                    .forEach(e -> result.add(TodoView.shared(e.toDomain(), roleByTodoId.get(e.getId()))));
        }
        result.sort(DISPLAY_ORDER);
        return result;
    }

    @Transactional
    public Todo createTodo(UUID memberId, TodoRequest request) {
        if (request.date().isBefore(LocalDate.now(clock.withZone(ZONE)))) {
            throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }
        if (request.labelId() != null) {
            requireOwnedLabel(memberId, request.labelId());
        }
        // 공개 생성 엔드포인트라 클라가 source를 임의 지정(ROUTINE/MEETING 위장 등) 못 하게 항상 MANUAL로 고정한다(L5).
        // ROUTINE/MEETING 출처는 각자의 전용 생성 경로(TodoEntity.createFromRoutine 등)로만 만들어진다.
        boolean pinned = request.pinned() != null ? request.pinned() : false;
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : 0;
        TodoEntity entity = TodoEntity.create(
                memberId,
                request.title(),
                request.date(),
                request.time(),
                request.placeName(),
                request.placeUrl(),
                request.lat(),
                request.lng(),
                request.labelId(),
                TodoSource.MANUAL,
                pinned,
                sortOrder
        );
        return todoRepository.save(entity).toDomain();
    }

    /**
     * 소유자 또는 EDITOR 이상 권한으로 수락한 공유 참여자만 수정 가능 (SHARE-2).
     * 삭제는 소유권 이전 개념이 없어 owner 전용으로 남겨둔다 — 참여자는 나가기(leave)만 가능.
     */
    @Transactional
    public Todo updateTodo(UUID memberId, UUID id, TodoUpdateRequest request) {
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        if (request.labelId() != null) {
            requireOwnedLabel(memberId, request.labelId());
        }
        TodoEntity entity = findEditableTodo(memberId, id);
        entity.update(request.title(), request.time(),
                request.placeName(), request.placeUrl(), request.lat(), request.lng(),
                request.labelId(), request.pinned(), request.sortOrder());
        if (request.completed() != null) {
            entity.setCompleted(request.completed(), LocalDateTime.now(clock));
        }

        // 알림(noti) 연계 지점 — 수정한 사람을 제외한 owner + 나머지 참여자에게 알림
        List<UUID> recipients = new ArrayList<>(todoParticipantRepository.findByTodoId(id).stream()
                .filter(p -> p.getInviteStatus() == InviteStatus.ACCEPTED)
                .map(TodoParticipantEntity::getMemberId)
                .filter(participantId -> !participantId.equals(memberId))
                .toList());
        if (!entity.getMemberId().equals(memberId)) {
            recipients.add(entity.getMemberId());
        }
        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(new TodoUpdatedEvent(id, entity.getTitle(), memberId, recipients));
        }

        return entity.toDomain();
    }

    @Transactional
    public void deleteTodo(UUID memberId, UUID id) {
        TodoEntity entity = todoRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        todoRepository.delete(entity);
    }

    private TodoEntity findEditableTodo(UUID memberId, UUID id) {
        return todoRepository.findByIdAndMemberId(id, memberId)
                .or(() -> findAsEditor(memberId, id))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Optional<TodoEntity> findAsEditor(UUID memberId, UUID id) {
        return todoParticipantRepository.findByTodoIdAndMemberId(id, memberId)
                .filter(p -> p.getInviteStatus() == InviteStatus.ACCEPTED)
                .filter(p -> p.getRole().isAtLeast(ParticipantRole.EDITOR))
                .flatMap(p -> todoRepository.findById(id));
    }

    /** labelId가 호출자 소유가 아니면 거부(M2) — 타 회원 라벨을 자기 todo에 박아 사용중 오차단·고아참조를 만드는 걸 막는다. */
    private void requireOwnedLabel(UUID memberId, UUID labelId) {
        labelRepository.findByIdAndMemberId(labelId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
