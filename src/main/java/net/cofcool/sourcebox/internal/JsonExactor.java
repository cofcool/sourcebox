package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for extracting values from JSON using JSONPath expressions.
 * <p>
 * Behavior:
 * - If the JSONPath matches, the matched value is returned as a JSON string.
 * - If the JSONPath does not match, the method returns the JSON literal "null".
 * - If the input JSON is invalid, an IOException is thrown.
 */
public final class JsonExactor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // keep dates as-is if any; do not fail on unknowns by default
        MAPPER.findAndRegisterModules();
    }

    private JsonExactor() {
    }

    /**
     * Extract by JSONPath and return the result as a compact JSON string.
     */
    public static String extract(String json, String jsonPath) throws IOException {
        return extract(json, jsonPath, false);
    }

    /**
     * Extract by JSONPath and return the result as JSON string. If pretty is true, output will be pretty-printed.
     */
    public static String extract(String json, String jsonPath, boolean pretty) throws IOException {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        if (jsonPath == null) {
            throw new IllegalArgumentException("jsonPath must not be null");
        }

        try {
            Object result = JsonPath.parse(json).read(jsonPath);

            // Convert result into Jackson-friendly types and serialize
            Object jacksonValue = MAPPER.convertValue(result, Object.class);

            if (pretty) {
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jacksonValue);
            }
            return MAPPER.writeValueAsString(jacksonValue);
        } catch (PathNotFoundException e) {
            // JSONPath didn't match anything -> return JSON null literal
            return "null";
        } catch (com.jayway.jsonpath.InvalidJsonException | JsonProcessingException e) {
            throw new IOException("Failed to parse JSON or convert extraction result: " + e.getMessage(), e);
        }
    }

    /**
     * Read JSON from a file and extract by JSONPath. Returns compact JSON string.
     */
    public static String extractFromFile(String filePath, String jsonPath) throws IOException {
        return extractFromFile(filePath, jsonPath, false);
    }

    /**
     * Read JSON from a file and extract by JSONPath. If pretty is true, output will be pretty-printed.
     */
    public static String extractFromFile(String filePath, String jsonPath, boolean pretty) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath must not be null");
        }
        String json = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        return extract(json, jsonPath, pretty);
    }
}
