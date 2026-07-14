package com.haeyaji.be.weather.client.kma;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 기상청 단기예보(getVilageFcst) JSON 응답 매핑. 불필요 필드는 무시.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaForecastResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    /**
     * 예보 한 칸. category 예: TMP(기온), TMX(최고), TMN(최저), POP(강수확률),
     * REH(습도), WSD(풍속), SKY(하늘상태), PTY(강수형태).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String category, String fcstDate, String fcstTime, String fcstValue) {
    }

    public boolean isSuccess() {
        return response != null
                && response.header() != null
                && "00".equals(response.header().resultCode());
    }

    public List<Item> items() {
        if (response == null || response.body() == null || response.body().items() == null
                || response.body().items().item() == null) {
            return List.of();
        }
        return response.body().items().item();
    }
}
