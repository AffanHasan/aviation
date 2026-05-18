package com.example.aviation.client;

import com.example.aviation.domain.StateVector;

import java.util.List;

public final class StateVectorConverter {

    private StateVectorConverter() {
    }

    @SuppressWarnings("unchecked")
    public static StateVector fromRawList(List<Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        return new StateVector(
                getString(raw, 0),
                getString(raw, 1),
                getString(raw, 2),
                getInteger(raw, 3),
                getInteger(raw, 4),
                getDouble(raw, 5),
                getDouble(raw, 6),
                getDouble(raw, 7),
                getBoolean(raw, 8),
                getDouble(raw, 9),
                getDouble(raw, 10),
                getDouble(raw, 11),
                getIntegerList(raw, 12),
                getDouble(raw, 13),
                getString(raw, 14),
                getBoolean(raw, 15),
                getInteger(raw, 16),
                getInteger(raw, 17)
        );
    }

    private static String getString(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return null;
        }
        return raw.get(index).toString();
    }

    private static Integer getInteger(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return null;
        }
        if (raw.get(index) instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private static Double getDouble(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return null;
        }
        if (raw.get(index) instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private static Boolean getBoolean(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return null;
        }
        if (raw.get(index) instanceof Boolean b) {
            return b;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> getIntegerList(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return null;
        }
        if (raw.get(index) instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Number)
                    .map(item -> ((Number) item).intValue())
                    .toList();
        }
        return null;
    }
}
