package com.haeyaji.be.recommend.service;

import com.haeyaji.be.recommend.client.kakao.KakaoLocalClient;
import com.haeyaji.be.recommend.domain.Coordinates;
import com.haeyaji.be.recommend.domain.Place;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 장소 검색·지오코딩 서비스. 컨트롤러의 요청을 카카오 로컬 클라이언트에 위임한다.
 * be는 nlp 대행 프록시라 실패는 클라이언트에서 빈 결과로 흡수된다(graceful).
 */
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;

    /** 키워드 검색 결과(최대 size). 실패·무결과 시 빈 리스트. */
    public List<Place> search(PlaceSearchQuery query) {
        return kakaoLocalClient.search(query);
    }

    /** 지역·장소명 → 대표 좌표. 미발견·실패 시 {@link Optional#empty()}. */
    public Optional<Coordinates> geocode(String query) {
        return kakaoLocalClient.geocode(query);
    }
}
