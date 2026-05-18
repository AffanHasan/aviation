package com.example.aviation.domain;

import java.util.List;

public record StateVector(
        String icao24,
        String callsign,
        String originCountry,
        Integer timePosition,
        Integer lastContact,
        Double longitude,
        Double latitude,
        Double baroAltitude,
        Boolean onGround,
        Double velocity,
        Double trueTrack,
        Double verticalRate,
        List<Integer> sensors,
        Double geoAltitude,
        String squawk,
        Boolean spi,
        Integer positionSource,
        Integer category
) {
}
