package com.haeyaji.be.common.exception;

import com.haeyaji.be.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ======================================================================
    // 1. 커스텀 예외
    // ======================================================================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ResponseCode code = e.getErrorCode();
        logByStatus(e, code, null);

        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.fail(code));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleConstraintViolation(
            ConstraintViolationException e
    ) {
        List<ValidationError> errors = e.getConstraintViolations().stream()
                .map(v -> ValidationError.of(extractField(v.getPropertyPath()), v.getMessage()))
                .toList();

        logClientError(e, ErrorCode.INVALID_PARAMETER, errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(errors, ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e
    ) {
        ValidationError error = ValidationError.of(e.getName(), "요청 값의 타입이 올바르지 않습니다.");
        logClientError(e, ErrorCode.INVALID_PARAMETER, "parameter=" + e.getName());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(List.of(error), ErrorCode.INVALID_PARAMETER));
    }

    /**
     * 최후의 안전망. 여기 도달하면 무조건 서버 결함 → ERROR + 전체 스택트레이스.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        logServerError(e, ErrorCode.INTERNAL_ERROR);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }

    // ======================================================================
    // 2. Spring MVC 표준 예외 — protected 메서드 오버라이드
    // ======================================================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request
    ) {
        List<ValidationError> errors = toValidationErrors(ex.getBindingResult());
        logClientError(ex, ErrorCode.INVALID_PARAMETER, errors);

        return handleExceptionInternal(
                ex, ApiResponse.fail(errors, ErrorCode.INVALID_PARAMETER), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request
    ) {
        List<ValidationError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ValidationError.of(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();

        logClientError(ex, ErrorCode.INVALID_PARAMETER, errors);

        return handleExceptionInternal(
                ex, ApiResponse.fail(errors, ErrorCode.INVALID_PARAMETER), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request
    ) {
        ValidationError error =
                ValidationError.of(ex.getParameterName(), "필수 요청 파라미터가 누락되었습니다.");
        logClientError(ex, ErrorCode.INVALID_PARAMETER, "parameter=" + ex.getParameterName());

        return handleExceptionInternal(
                ex, ApiResponse.fail(List.of(error), ErrorCode.INVALID_PARAMETER), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request
    ) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null && response.isCommitted()) {
                log.warn("event=RESPONSE_COMMITTED exception={} msg=\"{}\"",
                        ex.getClass().getSimpleName(), ex.getMessage());
                return null;
            }
        }

        if (statusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR) && body == null) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }

        // 개별 오버라이드를 거치지 않은 Spring MVC 예외 → 공통 포맷 변환 + 일괄 로깅
        if (!(body instanceof ApiResponse<?>)) {
            ErrorCode code = mapToErrorCode(statusCode);
            logByStatus(ex, code, null);
            body = ApiResponse.fail(code);
        }

        return createResponseEntity(body, headers, statusCode, request);
    }

    // ======================================================================
    // 3. 로깅 헬퍼 — 로그 정책의 단일 진입점
    //
    //    포맷: logfmt(key=value). 요청 컨텍스트(requestId, method, uri, ip)는
    //    MdcLoggingFilter 가 MDC 에 넣어 두므로 메시지에 중복 기록하지 않는다.
    //
    //    정책:
    //      - 4xx (클라이언트 잘못) → WARN, 스택트레이스 없음 (노이즈/비용 절감)
    //      - 5xx (서버 결함)      → ERROR, 스택트레이스 포함 (알림 트리거 대상)
    // ======================================================================

    private void logByStatus(Exception ex, ResponseCode code, @Nullable Object detail) {
        if (code.getStatus().is5xxServerError()) {
            logServerError(ex, code);
        } else {
            logClientError(ex, code, detail);
        }
    }

    private void logClientError(Exception ex, ResponseCode code, @Nullable Object detail) {
        log.warn("event=CLIENT_ERROR code={} status={} exception={} detail=\"{}\" msg=\"{}\"",
                code.name(),
                code.getStatus().value(),
                ex.getClass().getSimpleName(),
                detail == null ? "-" : detail,
                rootCauseMessage(ex));
    }

    private void logServerError(Exception ex, ResponseCode code) {
        log.error("event=SERVER_ERROR code={} status={} exception={} msg=\"{}\"",
                code.name(),
                code.getStatus().value(),
                ex.getClass().getSimpleName(),
                rootCauseMessage(ex),
                ex);  // 마지막 인자 Throwable → 스택트레이스 출력
    }

    /**
     * HttpMessageNotReadableException 처럼 래핑이 깊은 예외에서
     * 실제 원인 메시지를 뽑아낸다. (내부 클래스명/경로 노출 방지를 위해 응답에는 미포함)
     */
    private String rootCauseMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null ? root.getClass().getSimpleName() : message;
    }

    // ======================================================================
    // private helpers
    // ======================================================================

    private List<ValidationError> toValidationErrors(BindingResult bindingResult) {
        Stream<ValidationError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(e -> ValidationError.of(e.getField(), e.getDefaultMessage()));

        Stream<ValidationError> globalErrors = bindingResult.getGlobalErrors().stream()
                .map(e -> ValidationError.of(e.getObjectName(), e.getDefaultMessage()));

        return Stream.concat(fieldErrors, globalErrors).toList();
    }

    private ErrorCode mapToErrorCode(HttpStatusCode status) {
        if (status.equals(HttpStatus.METHOD_NOT_ALLOWED)) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status.equals(HttpStatus.NOT_FOUND)) {
            return ErrorCode.NOT_FOUND;
        }
        if (status.is4xxClientError()) {
            return ErrorCode.INVALID_PARAMETER;
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    private String extractField(Path propertyPath) {
        String path = propertyPath.toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot == -1 ? path : path.substring(lastDot + 1);
    }
}