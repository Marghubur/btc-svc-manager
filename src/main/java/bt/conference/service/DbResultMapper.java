package bt.conference.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DbResultMapper {
    private final ObjectMapper mapper;

    @Autowired
    public DbResultMapper(ObjectMapper mapper) {
        this.mapper = mapper.copy()
                .registerModule(new JavaTimeModule()) // 🔑 this is mandatory
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @SuppressWarnings("unchecked")
    public <T> T mapResult(Map<String, Object> result, String key, Class<T> clazz) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get(key);
        if (rows == null || rows.isEmpty()) {
            return null; // no data
        }

        Map<String, Object> firstRow = rows.get(0);
        return mapper.convertValue(firstRow, clazz);
    }

    // Overload for List<T> type safety using TypeReference
    public <T> List<T> mapListResult(Map<String, Object> result, String key, Class<T> clazz) {
        var typeRef = new TypeReference<List<T>>() {};
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get(key);
        if (data == null) {
            return List.of();
        }
        return data.stream()
                .map(row -> mapper.convertValue(row, clazz))
                .toList();
    }

    public <T> T convertValue(Object result, Class<T> clazz) {
        return mapper.convertValue(result, clazz);
    }

    public <T> List<T> convertListValue(Object result, Class<T> clazz) {
        return mapper.convertValue(
                result,
                mapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }

    public <T> T readValue(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }

    public <T> T readValue(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return mapper.readValue(json, typeReference);
    }

    public String writeValueAsString(Object data) throws JsonProcessingException {
        return mapper.writeValueAsString(data);
    }
}
