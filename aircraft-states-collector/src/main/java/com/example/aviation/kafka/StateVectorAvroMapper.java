package com.example.aviation.kafka;

import com.example.aviation.avro.StateVectorAvro;
import com.example.aviation.avro.StateVectorResponseAvro;
import com.example.aviation.domain.StateVector;
import com.example.aviation.domain.StateVectorResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StateVectorAvroMapper {

    public StateVectorResponseAvro toAvro(StateVectorResponse response) {
        StateVectorResponseAvro avro = new StateVectorResponseAvro();
        avro.setTime(response.time());

        if (response.states() != null) {
            List<StateVectorAvro> states = response.states().stream()
                    .map(this::toAvro)
                    .collect(Collectors.toList());
            avro.setStates(states);
        } else {
            avro.setStates(null);
        }

        return avro;
    }

    private StateVectorAvro toAvro(StateVector sv) {
        StateVectorAvro avro = new StateVectorAvro();
        avro.setIcao24(sv.icao24());
        avro.setCallsign(sv.callsign());
        avro.setOriginCountry(sv.originCountry());
        avro.setTimePosition(sv.timePosition());
        avro.setLastContact(sv.lastContact());
        avro.setLongitude(sv.longitude());
        avro.setLatitude(sv.latitude());
        avro.setBaroAltitude(sv.baroAltitude());
        avro.setOnGround(sv.onGround());
        avro.setVelocity(sv.velocity());
        avro.setTrueTrack(sv.trueTrack());
        avro.setVerticalRate(sv.verticalRate());
        avro.setSensors(sv.sensors());
        avro.setGeoAltitude(sv.geoAltitude());
        avro.setSquawk(sv.squawk());
        avro.setSpi(sv.spi());
        avro.setPositionSource(sv.positionSource());
        avro.setCategory(sv.category());
        return avro;
    }
}
