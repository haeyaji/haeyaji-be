package com.haeyaji.be.recommend.application.port.out;

import com.haeyaji.be.recommend.domain.Coordinates;
import com.haeyaji.be.recommend.domain.Place;

import java.util.List;
import java.util.Optional;

/**
 * 아웃바운드 포트: 장소 검색·지오코딩 제공자 (카카오 로컬).
 * <p>be는 nlp 대행 프록시라 검색 실패는 예외 대신 빈 결과로 흡수한다(graceful).
 */
public interface PlaceProvider {

    /** 키워드 검색 결과(최대 size). 실패·무결과 시 빈 리스트. */
    List<Place> search(PlaceSearchQuery query);

    /** 지역·장소명 → 대표 좌표. 미발견·실패 시 {@link Optional#empty()}. */
    Optional<Coordinates> geocode(String query);
}
