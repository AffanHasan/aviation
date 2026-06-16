package aviation.domain;

import java.io.Serializable;

public record AircraftState(AircraftAttributes attributes) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static AircraftState empty() {
        return new AircraftState(null);
    }
}
