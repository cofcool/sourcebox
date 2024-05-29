package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import org.apache.commons.io.FileUtils;

public class JsonFormatter implements Tool {

    @SuppressWarnings("rawtypes")
    private static final Class<LinkedHashMap> OBJ_TYPE = LinkedHashMap.class;
    private final ObjectMapper objectMapper = init();

    @Override
    public ToolName name() {
        return ToolName.json;
    }

    private ObjectMapper init() {
        return new ObjectMapper();
    }

    @Override
    public void run(Args args) throws Exception {
        var json = args.readArg("json");
        var path = args.readArg("path");
        var jsonLine = args.readArg("jsonl").val();

        LinkedHashMap<?, ?> obj;
        if (json.isPresent()) {
            obj = objectMapper.readValue(json.val(), OBJ_TYPE);
        } else if (path.isPresent()) {
            obj = switch (jsonLine) {
                case "none" -> objectMapper.readValue(new File(path.val()), OBJ_TYPE);
                case "line" -> {
                    var tmp = new LinkedHashMap<String, Object>();
                    var  lines = FileUtils.readLines(new File(path.val()), StandardCharsets.UTF_8)
                        .stream()
                        .map(a -> {
                            try {
                                return objectMapper.readValue(a, OBJ_TYPE);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .toList();
                    for (int i = 0; i < lines.size(); i++) {
                        tmp.put(String.valueOf(i), lines.get(i));
                    }
                    yield tmp;
                }
                case "idline" -> {
                    var tmp = new LinkedHashMap<String, Object>();
                    var lines = FileUtils.readLines(new File(path.val()), StandardCharsets.UTF_8).stream().filter(a -> !a.isBlank()).toList();
                    for (int i = 0; i < lines.size(); i = i+2) {
                        tmp.put(lines.get(i), objectMapper.readValue(lines.get(i+1), OBJ_TYPE));
                    }
                    yield tmp;
                }
                default ->
                    throw new IllegalArgumentException("JSON Line " + jsonLine + " error");
            };
        } else {
            throw new IllegalArgumentException("Json or file path must not be null");
        }

        var writer = objectMapper.writer();

        if (args.readArg("pretty").test(Boolean::parseBoolean)) {
            writer = writer.with(SerializationFeature.INDENT_OUTPUT);
        } else {
            writer = writer.without(SerializationFeature.INDENT_OUTPUT);
        }

        var result = writer.writeValueAsString(obj);

        args.getContext().write(path.val(), result);
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("json", null, "json string", false, "'{}'"))
            .arg(new Arg("path", null, "json file path", false, "./demo.json"))
            .arg(new Arg("pretty", "true", "", false, null))
            .arg(new Arg("jsonl", "none", "json line format: none, line, idline. Only supports json file", false, null))
            .alias("json", name(), "path", null)
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
    }
}
