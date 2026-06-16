package aviation.domain;

import java.io.Serializable;

public record AircraftStateEvent(AircraftAttributes attributes) implements Serializable {

    private static final long serialVersionUID = 1L;
}
