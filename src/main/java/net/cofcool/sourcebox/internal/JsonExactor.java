package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonExactor implements Tool {

    @Override
    public ToolName name() {
        return ToolName.jsonex;
    }

    @Override
    public void run(Args args) throws Exception {
        var json = args.readArg("in").requiredVal("json must not be empty");
        var jsonpath = args.readArg("jsonpath").requiredVal("jsonpath must not be empty");

        if (!json.startsWith("{")) {
            json = FileUtils.readFileToString(new File(json), StandardCharsets.UTF_8);
        }

        var obj = JsonUtil.extract(json, jsonpath);
        if (obj instanceof List) {
            obj = ((List<?>) obj).stream().map(Object::toString).collect(Collectors.joining("\n"));
        }
        args.getContext().write(obj);
    }

    @Override
    public Args config() {
        return new Args()
                .arg(new Arg("in", null, "json string", false, "'{}' or /demo.json"))
                .arg(new Arg("jsonpath", null, "json path", true, "$.a.b"))
                .runnerTypes(EnumSet.allOf(RunnerType.class));
    }
}
