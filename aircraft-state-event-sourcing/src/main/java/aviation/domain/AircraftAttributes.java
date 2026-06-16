package aviation.domain;

import java.io.Serializable;
import java.util.List;

public record AircraftAttributes(
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
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
