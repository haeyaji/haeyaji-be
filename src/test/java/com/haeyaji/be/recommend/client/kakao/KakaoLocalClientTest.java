package com.haeyaji.be.recommend.client.kakao;

import com.haeyaji.be.recommend.service.PlaceSearchQuery;
import com.haeyaji.be.recommend.domain.Coordinates;
import com.haeyaji.be.recommend.domain.Place;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 카카오 로컬 클라이언트 매핑·요청 조립 검증. ExchangeFunction 스텁으로 HTTP 없이 응답을 흉내낸다.
 */
class KakaoLocalClientTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final AtomicReference<ClientRequest> lastRequest = new AtomicReference<>();

    /** 고정 JSON을 200으로 돌려주는 WebClient(요청은 lastRequest에 캡처). */
    private WebClient stubClient(String json) {
        ExchangeFunction exchange = request -> {
            lastRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(json)
                    .build());
        };
        return WebClient.builder().baseUrl("http://kakao.test").exchangeFunction(exchange).build();
    }

    private KakaoLocalClient client(String json) {
        return new KakaoLocalClient(mapper, stubClient(json), "test-key");
    }

    private static final String ONE_PLACE = """
            {"documents":[{
              "place_name":"스타벅스 강남점",
              "category_name":"음식점 > 카페 > 커피전문점 > 스타벅스",
              "category_group_code":"CE7",
              "category_group_name":"카페",
              "address_name":"서울 강남구 역삼동 123",
              "road_address_name":"서울 강남구 테헤란로 100",
              "x":"127.0276",
              "y":"37.4979",
              "place_url":"http://place.map.kakao.com/123",
              "distance":"350"
            }]}""";

    @Test
    void 검색_응답을_Place로_매핑한다() {
        List<Place> places = client(ONE_PLACE).search(
                new PlaceSearchQuery("카페", 37.5, 127.0, 1000, 5, "distance", "CE7"));

        assertThat(places).hasSize(1);
        Place p = places.get(0);
        assertThat(p.name()).isEqualTo("스타벅스 강남점");
        assertThat(p.category()).isEqualTo("카페");                 // group_name 우선
        assertThat(p.address()).isEqualTo("서울 강남구 테헤란로 100"); // 도로명 우선
        assertThat(p.url()).isEqualTo("http://place.map.kakao.com/123");
        assertThat(p.distanceM()).isEqualTo(350);
        assertThat(p.x()).isEqualTo(127.0276); // 경도
        assertThat(p.y()).isEqualTo(37.4979);  // 위도
    }

    @Test
    void 그룹명_도로명이_없으면_전체분류명_지번주소로_폴백한다() {
        String json = """
                {"documents":[{
                  "place_name":"어느 관공서",
                  "category_name":"사회,공공기관 > 행정기관",
                  "category_group_name":"",
                  "address_name":"서울 종로구 세종로 1",
                  "road_address_name":"",
                  "x":"126.97","y":"37.57","place_url":"","distance":""
                }]}""";
        Place p = client(json).search(
                new PlaceSearchQuery("관공서", null, null, null, 5, "accuracy", null)).get(0);

        assertThat(p.category()).isEqualTo("사회,공공기관 > 행정기관");
        assertThat(p.address()).isEqualTo("서울 종로구 세종로 1");
        assertThat(p.distanceM()).isNull(); // 빈 문자열 → null
    }

    @Test
    void 중심좌표가_있으면_x는경도_y는위도로_보내고_distance정렬을_유지한다() {
        client(ONE_PLACE).search(
                new PlaceSearchQuery("카페", 37.5, 127.0, 1000, 5, "distance", null));

        String query = lastRequest.get().url().getQuery();
        assertThat(query).contains("x=127.0");   // 경도=lng
        assertThat(query).contains("y=37.5");     // 위도=lat
        assertThat(query).contains("radius=1000");
        assertThat(query).contains("sort=distance");
    }

    @Test
    void 중심좌표없이_distance정렬을_요청하면_accuracy로_강등한다() {
        client("{\"documents\":[]}").search(
                new PlaceSearchQuery("카페", null, null, 1000, 5, "distance", null));

        String query = lastRequest.get().url().getQuery();
        assertThat(query).contains("sort=accuracy");
        assertThat(query).doesNotContain("x=");
        assertThat(query).doesNotContain("radius=");
    }

    @Test
    void 지오코딩은_첫결과의_위경도를_좌표로_반환한다() {
        Optional<Coordinates> coord = client(ONE_PLACE).geocode("강남역");

        assertThat(coord).isPresent();
        assertThat(coord.get().lat()).isEqualTo(37.4979);
        assertThat(coord.get().lng()).isEqualTo(127.0276);
    }

    @Test
    void 지오코딩_결과가_없으면_empty() {
        assertThat(client("{\"documents\":[]}").geocode("없는곳")).isEmpty();
    }

    @Test
    void 키가_없으면_호출없이_검색은_빈리스트_지오코딩은_empty() {
        KakaoLocalClient noKey = new KakaoLocalClient(mapper, stubClient(ONE_PLACE), "");

        assertThat(noKey.search(new PlaceSearchQuery("카페", 37.5, 127.0, 1000, 5, "accuracy", null)))
                .isEmpty();
        assertThat(noKey.geocode("강남역")).isEmpty();
        assertThat(lastRequest.get()).isNull(); // 실제 호출 자체가 없었음
    }
}
