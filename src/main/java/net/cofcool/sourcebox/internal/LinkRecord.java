package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.LinkCategory;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;

@CustomLog
public class LinkRecord extends BaseRecord {

    @Override
    public ToolName name() {
        return ToolName.link;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
        var flag = new AtomicBoolean();

        args.readArg("import").ifPresent(a -> {
            flag.set(true);
            try {
                var root = JsonUtil.getObjectMapper().readTree(new File(a.val()));
                var d = new ArrayList<String[]>();

                extractBookmarks(root, d);

                d.forEach(u -> saveRecord(null, u[0], u[1], LinkCategory.bookmark.name()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return flag.get();
    }

    @Override
    protected void callAdd(Arg a) {
        saveRecord(null, a.val(), null, LinkCategory.bookmark.name());
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

    @Override
    public Args config() {
        return super.config()
            .arg(new Arg("import", null, "file path, supports: firefox bookmark file", false, null))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    @Override
    protected Type currentType() {
        return Type.link;
    }
}
