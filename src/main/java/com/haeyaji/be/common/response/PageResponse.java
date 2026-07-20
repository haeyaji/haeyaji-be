package com.haeyaji.be.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 응답 객체.
 * <p>
 * Spring Data의 {@link org.springframework.data.domain.Page}를 API 응답 모델로 변환하기 위한 DTO이다. 내부 구현인 {@code Page}를 외부에 직접 노출하지
 * 않고, 필요한 페이징 정보만 제공한다.
 *
 * @param <T> 페이지 데이터 타입
 */
public record PageResponse<T>(
        List<T> content,
        PageMetadata page
) {

    /**
     * {@link org.springframework.data.domain.Page}를 {@link PageResponse}로 변환한다.
     *
     * @param page 변환할 Spring Data Page 객체
     * @param <T>  페이지 데이터 타입
     * @return PageResponse
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            PageMetadata.from(page)
        );
    }

    public record PageMetadata(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public PageMetadata {
            if (page < 0) throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
            if (size <= 0) throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
            if (totalElements < 0) throw new IllegalArgumentException("총 요소 수는 0 이상이어야 합니다.");
            if (totalPages < 0) throw new IllegalArgumentException("총 페이지 수는 0 이상이어야 합니다.");
        }

        public static PageMetadata from(Page<?> page) {
            return new PageMetadata(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast(),
                    page.hasNext(),
                    page.hasPrevious()
            );
        }
    }
}