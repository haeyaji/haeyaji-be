package com.haeyaji.be.recommend.api.dto;

import com.haeyaji.be.recommend.domain.Coordinates;

/** GET /api/places/geocode 200 응답. */
public record CoordinatesResponse(double lat, double lng) {

    public static CoordinatesResponse from(Coordinates c) {
        return new CoordinatesResponse(c.lat(), c.lng());
    }
}
