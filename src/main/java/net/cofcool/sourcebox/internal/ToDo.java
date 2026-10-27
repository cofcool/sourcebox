package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.util.CsvParser;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class ToDo extends BaseRecord {

    @Override
    public ToolName name() {
        return ToolName.todo;
    }

    @Override
    protected Type currentType() {
        return Type.todo;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
        var flag = new AtomicBoolean();

        args.readArg("importBookmark").ifPresent(a -> {
            flag.set(true);
            try {
                var root = JsonUtil.getObjectMapper().readTree(new File(a.val()));
                var d = new ArrayList<String[]>();

                extractBookmarks(root, d);

                d.forEach(u -> saveRecord(null, u[0], u[1], null));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        args.readArg("importTodo").ifPresent(a -> {
            flag.set(true);
            try {
                FileUtils
                    .readLines(new File(a.val()), StandardCharsets.UTF_8)
                    .forEach(s -> {
                        var t =  CsvParser.parseLine(s);
                        var state = "todo";
                        var remark = "";
                        if (t.size() == 2) {
                            remark = t.get(1);
                        }
                        if (t.size() == 3) {
                            remark = t.get(1);
                            state = t.get(2);
                        }
                        saveRecord(ActionState.valueOf(state), t.get(0), remark, null);
                    });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return flag.get();
    }

    @Override
    public Args config() {
        return super.config()
            .arg(new Arg("importTodo", null, "csv file path, like: value,remark or value,remark,state ", false, null))
            .arg(new Arg("importBookmark", null, "file path, supports: firefox bookmark file", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    private void extractBookmarks(JsonNode node, List<String[]> urls) {
        if (node == null) {
            return;
        }

        if (node.has("uri")) {
            var title = node.has("title") ? node.get("title").asText() : "";
            var url = node.get("uri").asText();
            urls.add(new String[]{StringUtils.isBlank(title) ? url : title, url});
        }

        if (node.has("children")) {
            for (JsonNode child : node.get("children")) {
                extractBookmarks(child, urls);
            }
        }
    }

}
