package com.haeyaji.be.recommend.client.nlp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * nlp {@code POST /api/message} 응답 미러. be는 가공 없이 fe로 그대로 전달한다.
 * {@code todos/options/actions}는 nlp가 채우는 자유 구조라 그대로 passthrough(List&lt;Object&gt;).
 * nlp가 필드를 추가해도 깨지지 않게 미지정 필드는 무시.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NlpMessageResponse(
        String intent,
        String reply,
        List<Object> todos,
        List<Object> options,
        List<Object> actions
) {
}
