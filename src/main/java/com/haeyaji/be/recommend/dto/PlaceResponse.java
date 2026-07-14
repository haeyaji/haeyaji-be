package com.haeyaji.be.recommend.dto;

import com.haeyaji.be.recommend.domain.Place;

/** 장소 응답 (nlp Place 계약과 1:1, camelCase). x=경도, y=위도, distanceM nullable. */
public record PlaceResponse(
        String name,
        String category,
        String address,
        String url,
        Integer distanceM,
        Double x,
        Double y
) {

    public static PlaceResponse from(Place p) {
        return new PlaceResponse(p.name(), p.category(), p.address(), p.url(),
                p.distanceM(), p.x(), p.y());
    }
}
