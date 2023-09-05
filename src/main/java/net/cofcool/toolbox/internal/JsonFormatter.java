package net.cofcool.toolbox.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import net.cofcool.toolbox.Tool;
import net.cofcool.toolbox.ToolName;

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

        LinkedHashMap<?, ?> obj;
        if (json.isPresent()) {
            obj = objectMapper.readValue(json.val(), OBJ_TYPE);
        } else if (path.isPresent()) {
            obj = objectMapper.readValue(new File(path.val()), OBJ_TYPE);
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
            .arg(new Arg("json", null, "json string", false, "{}"))
            .arg(new Arg("path", null, "json file path", false, "./demo.json"))
            .arg(new Arg("pretty", "true", "", false, null))
            .alias("json", name(), "path", null)
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
    }
}
