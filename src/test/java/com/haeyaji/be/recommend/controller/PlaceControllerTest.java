package com.haeyaji.be.recommend.controller;

import com.haeyaji.be.recommend.dto.CoordinatesResponse;
import com.haeyaji.be.recommend.dto.PlacesResponse;
import com.haeyaji.be.recommend.domain.Coordinates;
import com.haeyaji.be.recommend.domain.Place;
import com.haeyaji.be.recommend.service.PlaceSearchQuery;
import com.haeyaji.be.recommend.service.PlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 프록시 컨트롤러 검증. service 스텁으로 매핑·상태코드(200/204)만 확인(프레임워크 바인딩은 제외).
 */
class PlaceControllerTest {

    /** search/geocode 반환값을 주입식으로 고정하는 스텁. */
    private PlaceService provider(List<Place> places, Optional<Coordinates> coord) {
        PlaceService mock = mock(PlaceService.class);
        when(mock.search(any(PlaceSearchQuery.class))).thenReturn(places);
        when(mock.geocode(anyString())).thenReturn(coord);
        return mock;
    }

    @Test
    void 검색은_provider결과를_places로_매핑한다() {
        Place place = new Place("스타벅스", "카페", "테헤란로 100",
                "http://place/1", 350, 127.0276, 37.4979);
        PlaceController controller = new PlaceController(provider(List.of(place), Optional.empty()));

        PlacesResponse body = controller.search("카페", 37.5, 127.0, 1000, 5, "distance", "CE7");

        assertThat(body.places()).hasSize(1);
        assertThat(body.places().get(0).name()).isEqualTo("스타벅스");
        assertThat(body.places().get(0).distanceM()).isEqualTo(350);
        assertThat(body.places().get(0).x()).isEqualTo(127.0276);
    }

    @Test
    void 검색_무결과는_빈배열_200() {
        PlaceController controller = new PlaceController(provider(List.of(), Optional.empty()));

        PlacesResponse body = controller.search("없는곳", null, null, null, 5, "accuracy", null);

        assertThat(body.places()).isEmpty();
    }

    @Test
    void 지오코딩_결과있으면_200과_좌표바디() {
        PlaceController controller = new PlaceController(
                provider(List.of(), Optional.of(new Coordinates(37.4979, 127.0276))));

        ResponseEntity<CoordinatesResponse> res = controller.geocode("강남역");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().lat()).isEqualTo(37.4979);
        assertThat(res.getBody().lng()).isEqualTo(127.0276);
    }

    @Test
    void 지오코딩_미발견이면_204_바디없음() {
        PlaceController controller = new PlaceController(provider(List.of(), Optional.empty()));

        ResponseEntity<CoordinatesResponse> res = controller.geocode("없는곳");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(res.getBody()).isNull();
    }
}
