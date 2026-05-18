package com.example.aviation.domain;

import java.util.List;

public record StateVectorResponse(Long time, List<StateVector> states) {
}
