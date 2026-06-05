package aviation.client;

import aviation.domain.StateVector;
import aviation.domain.StateVectorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class StateVectorConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StateVectorConverter() {
    }

    @SuppressWarnings("unchecked")
    public static StateVectorResponse fromJson(String json) {
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(json, Map.class);
            Long time = ((Number) root.get("time")).longValue();
            List<List<Object>> rawStates = (List<List<Object>>) root.get("states");
            List<StateVector> states = null;
            if (rawStates != null) {
                states = rawStates.stream()
                        .map(StateVectorConverter::fromRawList)
                        .toList();
            }
            return new StateVectorResponse(time, states);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenSky API response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static StateVector fromRawList(List<Object> raw) {
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
