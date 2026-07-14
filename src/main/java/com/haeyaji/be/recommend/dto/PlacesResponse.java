package com.haeyaji.be.recommend.dto;

import com.haeyaji.be.recommend.domain.Place;

import java.util.List;

/** GET /api/places/search 응답. envelope 없이 { "places": [...] } (weather와 동일 raw JSON). */
public record PlacesResponse(List<PlaceResponse> places) {

    public static PlacesResponse from(List<Place> places) {
        return new PlacesResponse(places.stream().map(PlaceResponse::from).toList());
    }
}
