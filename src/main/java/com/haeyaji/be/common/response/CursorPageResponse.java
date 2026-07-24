package com.haeyaji.be.common.response;

import java.util.List;

/**
 * 커서 페이지네이션 응답 객체.
 * <p>
 * "몇 번째 페이지"가 아니라 "마지막으로 조회한 지점(cursor)" 기준으로 다음 데이터를 이어서 가져오는 방식.
 * 계속 쌓이는 피드성 목록(알림 등)에 적합하며, {@link PageResponse}와 달리 전체 개수·페이지 수는 제공하지 않는다.
 *
 * @param <T> 응답 데이터 타입
 * @param <C> 커서로 쓰이는 값의 타입 (예: UUID, Long)
 */
public record CursorPageResponse<T, C>(
        List<T> content,
        C nextCursor,
        boolean hasNext
) {

    /**
     * @param content   이번 페이지 데이터
     * @param nextCursor 다음 요청 시 그대로 넘길 커서값. 더 가져올 게 없으면 null
     * @param hasNext   다음 페이지 존재 여부
     */
    public static <T, C> CursorPageResponse<T, C> of(List<T> content, C nextCursor, boolean hasNext) {
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
