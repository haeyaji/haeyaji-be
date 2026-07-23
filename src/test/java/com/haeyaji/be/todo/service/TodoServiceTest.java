package com.haeyaji.be.todo.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.repository.TodoEntity;
import com.haeyaji.be.todo.repository.TodoRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TodoServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private final Clock fixedClock = Clock.fixed(
            ZonedDateTime.of(2026, 7, 22, 10, 0, 0, 0, ZONE).toInstant(), ZONE);

    private TodoRequest request(LocalDate date, TodoSource source, Boolean pinned, Integer sortOrder) {
        return new TodoRequest("제목", date, null, null, null, null, null, null, source, pinned, sortOrder);
    }

    private TodoUpdateRequest updateRequest(String title, Boolean pinned, Integer sortOrder, Boolean completed) {
        return new TodoUpdateRequest(title, null, null, null, null, null, null, pinned, sortOrder, completed);
    }

    private TodoEntity entity(String title, LocalTime startTime, String placeName, String placeUrl,
            Double lat, Double lng, boolean pinned, int sortOrder) {
        return TodoEntity.create(MEMBER_ID, title, TODAY, startTime, placeName, placeUrl, lat, lng, null,
                TodoSource.MANUAL, pinned, sortOrder);
    }

    @Test
    void 과거_날짜로_추가하면_예외() {
        TodoRepository repo = mock(TodoRepository.class);
        TodoService service = new TodoService(repo, fixedClock);

        assertThatThrownBy(() -> service.createTodo(MEMBER_ID, request(TODAY.minusDays(1), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAST_DATE_NOT_ALLOWED);
    }

    @Test
    void 추가시_출처_고정여부_정렬순서_미지정이면_기본값() {
        TodoRepository repo = mock(TodoRepository.class);
        when(repo.save(any(TodoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TodoService service = new TodoService(repo, fixedClock);

        Todo todo = service.createTodo(MEMBER_ID, request(TODAY, null, null, null));

        assertThat(todo.memberId()).isEqualTo(MEMBER_ID);
        assertThat(todo.source()).isEqualTo(TodoSource.MANUAL);
        assertThat(todo.pinned()).isFalse();
        assertThat(todo.sortOrder()).isEqualTo(0);
    }

    @Test
    void 추가시_값을_주면_그대로_반영() {
        TodoRepository repo = mock(TodoRepository.class);
        when(repo.save(any(TodoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TodoService service = new TodoService(repo, fixedClock);

        Todo todo = service.createTodo(MEMBER_ID, request(TODAY, TodoSource.AI, true, 5));

        assertThat(todo.source()).isEqualTo(TodoSource.AI);
        assertThat(todo.pinned()).isTrue();
        assertThat(todo.sortOrder()).isEqualTo(5);
    }

    @Test
    void 등록시_라벨을_지정하면_반영된다() {
        TodoRepository repo = mock(TodoRepository.class);
        when(repo.save(any(TodoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        TodoService service = new TodoService(repo, fixedClock);
        UUID labelId = UUID.randomUUID();

        Todo todo = service.createTodo(MEMBER_ID, new TodoRequest("제목", TODAY, null, null, null, null, null,
                labelId, null, null, null));

        assertThat(todo.labelId()).isEqualTo(labelId);
    }

    @Test
    void 수정시_라벨을_지정하면_반영된다() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        TodoEntity existing = entity("제목", null, null, null, null, null, false, 0);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        TodoService service = new TodoService(repo, fixedClock);
        UUID labelId = UUID.randomUUID();

        Todo todo = service.updateTodo(MEMBER_ID, id,
                new TodoUpdateRequest(null, null, null, null, null, null, labelId, null, null, null));

        assertThat(todo.labelId()).isEqualTo(labelId);
    }

    @Test
    void 날짜로_조회하면_저장된_할일을_반환한다() {
        TodoRepository repo = mock(TodoRepository.class);
        TodoEntity saved = entity("제목", null, null, null, null, null, true, 1);
        when(repo.findByMemberIdAndTodoDateOrderByPinnedDescSortOrderAscCreatedAtAsc(MEMBER_ID, TODAY)).thenReturn(List.of(saved));
        TodoService service = new TodoService(repo, fixedClock);

        List<Todo> todos = service.getTodosByDate(MEMBER_ID, TODAY);

        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).title()).isEqualTo("제목");
    }

    @Test
    void 수정시_존재하지_않으면_예외() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        TodoService service = new TodoService(repo, fixedClock);

        assertThatThrownBy(() -> service.updateTodo(MEMBER_ID, id, updateRequest(null, true, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 다른_회원의_할일을_수정하려하면_예외() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, otherMemberId)).thenReturn(Optional.empty());
        TodoService service = new TodoService(repo, fixedClock);

        assertThatThrownBy(() -> service.updateTodo(otherMemberId, id, updateRequest(null, true, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정시_제목이_공백이면_예외() {
        TodoRepository repo = mock(TodoRepository.class);
        TodoService service = new TodoService(repo, fixedClock);

        assertThatThrownBy(() -> service.updateTodo(MEMBER_ID, UUID.randomUUID(), updateRequest("   ", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 부분수정시_보내지_않은_필드는_유지된다() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        TodoEntity existing = entity("기존제목", LocalTime.of(9, 0), "기존장소", "http://place",
                37.5, 127.0, false, 3);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        TodoService service = new TodoService(repo, fixedClock);

        Todo todo = service.updateTodo(MEMBER_ID, id, updateRequest(null, true, null, null));

        assertThat(todo.title()).isEqualTo("기존제목");
        assertThat(todo.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(todo.placeName()).isEqualTo("기존장소");
        assertThat(todo.placeUrl()).isEqualTo("http://place");
        assertThat(todo.lat()).isEqualTo(37.5);
        assertThat(todo.lng()).isEqualTo(127.0);
        assertThat(todo.sortOrder()).isEqualTo(3);
        assertThat(todo.pinned()).isTrue();
    }

    @Test
    void 완료처리시_상태와_완료시각이_기록된다() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        TodoEntity existing = entity("제목", null, null, null, null, null, false, 0);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        TodoService service = new TodoService(repo, fixedClock);

        Todo todo = service.updateTodo(MEMBER_ID, id, updateRequest(null, null, null, true));

        assertThat(todo.status()).isEqualTo(TodoStatus.DONE);
        assertThat(todo.endedAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    void 완료취소시_상태와_완료시각이_초기화된다() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        TodoEntity existing = entity("제목", null, null, null, null, null, false, 0);
        existing.setCompleted(true, LocalDateTime.now(fixedClock));
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        TodoService service = new TodoService(repo, fixedClock);

        Todo todo = service.updateTodo(MEMBER_ID, id, updateRequest(null, null, null, false));

        assertThat(todo.status()).isEqualTo(TodoStatus.TODO);
        assertThat(todo.endedAt()).isNull();
    }

    @Test
    void 삭제시_존재하지_않으면_예외() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.empty());
        TodoService service = new TodoService(repo, fixedClock);

        assertThatThrownBy(() -> service.deleteTodo(MEMBER_ID, id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 삭제시_존재하면_리포지토리에서_삭제된다() {
        TodoRepository repo = mock(TodoRepository.class);
        UUID id = UUID.randomUUID();
        TodoEntity existing = entity("제목", null, null, null, null, null, false, 0);
        when(repo.findByIdAndMemberId(id, MEMBER_ID)).thenReturn(Optional.of(existing));
        TodoService service = new TodoService(repo, fixedClock);

        service.deleteTodo(MEMBER_ID, id);

        verify(repo).delete(existing);
    }
}
