package com.haeyaji.be.todo.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.member.domain.MemberRole;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.todo.domain.Todo;
import com.haeyaji.be.todo.domain.TodoSource;
import com.haeyaji.be.todo.domain.TodoStatus;
import com.haeyaji.be.todo.dto.TodoListResponse;
import com.haeyaji.be.todo.dto.TodoRequest;
import com.haeyaji.be.todo.dto.TodoResponse;
import com.haeyaji.be.todo.dto.TodoUpdateRequest;
import com.haeyaji.be.todo.service.TodoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * service 스텁으로 매핑·ApiResponse 래핑만 확인(프레임워크 바인딩은 제외).
 */
class TodoControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 22);
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final CustomUserDetails USER = new CustomUserDetails(MEMBER_ID, MemberRole.ROLE_USER);

    private Todo todo(String title, TodoStatus status) {
        return Todo.builder()
                .id(UUID.randomUUID())
                .title(title)
                .todoDate(DATE)
                .source(TodoSource.MANUAL)
                .status(status)
                .pinned(false)
                .sortOrder(0)
                .build();
    }

    @Test
    void 목록조회는_완료_전체_개수를_함께_반환한다() {
        TodoService service = mock(TodoService.class);
        when(service.getTodosByDate(MEMBER_ID, DATE)).thenReturn(List.of(
                todo("완료된일", TodoStatus.DONE),
                todo("안한일", TodoStatus.TODO)
        ));
        TodoController controller = new TodoController(service);

        ApiResponse<TodoListResponse> response = controller.getTodos(USER, DATE);

        assertThat(response.success()).isTrue();
        assertThat(response.data().total()).isEqualTo(2);
        assertThat(response.data().completed()).isEqualTo(1);
        assertThat(response.data().tasks()).hasSize(2);
    }

    @Test
    void 추가는_생성된_할일을_담아_반환한다() {
        TodoService service = mock(TodoService.class);
        TodoRequest request = new TodoRequest("새일", DATE, null, null, null, null, null, null, null, null, null);
        when(service.createTodo(MEMBER_ID, request)).thenReturn(todo("새일", TodoStatus.TODO));
        TodoController controller = new TodoController(service);

        ApiResponse<TodoResponse> response = controller.createTodo(USER, request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("새일");
    }

    @Test
    void 추가는_201로_매핑된다() throws NoSuchMethodException {
        ResponseStatus status = TodoController.class
                .getMethod("createTodo", CustomUserDetails.class, TodoRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertThat(status.value()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 수정은_수정된_할일을_담아_반환한다() {
        TodoService service = mock(TodoService.class);
        UUID id = UUID.randomUUID();
        TodoUpdateRequest request = new TodoUpdateRequest(null, null, null, null, null, null, null, true, null, null);
        when(service.updateTodo(MEMBER_ID, id, request)).thenReturn(todo("수정된일", TodoStatus.TODO));
        TodoController controller = new TodoController(service);

        ApiResponse<TodoResponse> response = controller.updateTodo(USER, id, request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().title()).isEqualTo("수정된일");
    }

    @Test
    void 삭제는_서비스에_위임하고_데이터없이_반환한다() {
        TodoService service = mock(TodoService.class);
        UUID id = UUID.randomUUID();
        TodoController controller = new TodoController(service);

        ApiResponse<Void> response = controller.deleteTodo(USER, id);

        verify(service).deleteTodo(MEMBER_ID, id);
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
    }
}
