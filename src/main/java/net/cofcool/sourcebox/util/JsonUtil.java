package net.cofcool.sourcebox.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import lombok.SneakyThrows;

public class JsonUtil {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static ObjectMapper enableTimeModule(ObjectMapper objectMapper) {
        return objectMapper.registerModule(new JavaTimeModule());
    }

    @SneakyThrows
    public static <T> List<T> toPojoList(byte[] bytes, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(bytes, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    @SneakyThrows
    public static <T> T toPojo(byte[] bytes, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(bytes, clazz);
    }

    @SneakyThrows
    public static <T> T toPojo(String json, Class<T> clazz) {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    @SneakyThrows
    public static String toJson(Object val) {
        return OBJECT_MAPPER.writeValueAsString(val);
    }
}
