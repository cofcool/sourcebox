package net.cofcool.sourcebox.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.util.CsvParser;
import net.cofcool.sourcebox.util.JsonUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class ToDo extends BaseRecord {

    private static final int KEY_UP = 1001;
    private static final int KEY_DOWN = 1002;
    private static final int KEY_ESCAPE = 1003;

    @Override
    public ToolName name() {
        return ToolName.todo;
    }

    private void runTui() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            Attributes originalAttributes = terminal.enterRawMode();
            try {
                terminal.writer().print("\u001b[?25l");
                terminal.writer().flush();
                new TodoTui(terminal).run();
            } finally {
                terminal.setAttributes(originalAttributes);
                terminal.writer().print("\u001b[0m\u001b[?25h");
                terminal.writer().flush();
            }
        }
    }

    private final class TodoTui {

        private final Terminal terminal;
        private final List<ActionRecord> todos = new ArrayList<>();
        private int selectedIndex;
        private String status = "";
        private boolean adding;
        private int focusedField;
        private final StringBuilder title = new StringBuilder();
        private final StringBuilder description = new StringBuilder();

        private TodoTui(Terminal terminal) {
            this.terminal = terminal;
        }

        private void run() throws IOException {
            refreshTodos();
            while (true) {
                render();
                int key = readKey();
                if (key == -1 || (!adding && (key == 'q' || key == 'Q'))) {
                    return;
                }

                if (adding) {
                    handleFormKey(key);
                } else {
                    handleListKey(key);
                }
            }
        }

        private void handleListKey(int key) throws IOException {
            if (key == KEY_UP && selectedIndex > 0) {
                selectedIndex--;
            } else if (key == KEY_DOWN && selectedIndex < todos.size() - 1) {
                selectedIndex++;
            } else if (key == 'd' || key == 'D') {
                markSelectedDone();
            } else if (key == 'a' || key == 'A') {
                adding = true;
                focusedField = 0;
                title.setLength(0);
                description.setLength(0);
                status = "";
            }
        }

        private void handleFormKey(int key) throws IOException {
            if (key == KEY_ESCAPE) {
                adding = false;
                status = "已取消添加";
                return;
            }
            if (key == '\t') {
                focusedField = (focusedField + 1) % 3;
                return;
            }
            if (key == KEY_UP || key == KEY_DOWN) {
                focusedField = (focusedField + (key == KEY_DOWN ? 1 : 2)) % 3;
                return;
            }
            if (key == '\r' || key == '\n') {
                if (focusedField < 2) {
                    focusedField++;
                } else {
                    addTodo();
                }
                return;
            }
            if (focusedField == 2) {
                return;
            }

            StringBuilder field = focusedField == 0 ? title : description;
            if (key == 8 || key == 127) {
                if (!field.isEmpty()) {
                    field.deleteCharAt(field.length() - 1);
                }
            } else if (key >= 32 && key != 127) {
                field.append((char) key);
            }
        }

        private void addTodo() throws IOException {
            if (StringUtils.isBlank(title)) {
                status = "标题不能为空";
                focusedField = 0;
                return;
            }
            saveRecord(ActionState.todo, title.toString(), description.toString(), null);
            adding = false;
            status = "已添加";
            refreshTodos();
        }

        private void markSelectedDone() throws IOException {
            if (todos.isEmpty()) {
                status = "没有可完成的待办";
                return;
            }
            ActionRecord selected = todos.get(selectedIndex);
            updateState(selected.id(), ActionState.done);
            status = "已完成: " + selected.name();
            refreshTodos();
        }

        private void refreshTodos() {
            todos.clear();
            todos.addAll(findActions(Arg.of("find", "state="+ActionState.todo.name())));
            selectedIndex = Math.clamp(todos.size() - 1, 0, selectedIndex);
        }

        private int readKey() throws IOException {
            int key = terminal.reader().read();
            if (key != 27) {
                return key;
            }
            int next = terminal.reader().read(100);
            if (next != '[') {
                return KEY_ESCAPE;
            }
            int arrow = terminal.reader().read(100);
            return switch (arrow) {
                case 'A' -> KEY_UP;
                case 'B' -> KEY_DOWN;
                default -> KEY_ESCAPE;
            };
        }

        private void render() {
            var writer = terminal.writer();
            writer.print("\u001b[H\u001b[2J");
            writer.println("待办事项");
            writer.println("↑/↓ 选择  D 完成  A 添加  q 退出");
            writer.println("─".repeat(Math.clamp(terminal.getWidth(), 1, 60)));

            if (adding) {
                renderForm(writer);
            } else if (todos.isEmpty()) {
                writer.println("暂无待办");
            } else {
                int visibleRows = Math.max(1, terminal.getHeight() - 7);
                int firstVisible = Math.max(0, Math.min(
                    selectedIndex - visibleRows + 1,
                    todos.size() - visibleRows
                ));
                int lastVisible = Math.min(todos.size(), firstVisible + visibleRows);
                for (int i = firstVisible; i < lastVisible; i++) {
                    ActionRecord todo = todos.get(i);
                    String marker = i == selectedIndex ? "> " : "  ";
                    String details = StringUtils.isBlank(todo.remark()) ? "" : " - " + todo.remark();
                    writer.println(marker + truncate(safe(todo.name()) + safe(details)));
                }
            }
            writer.println();
            writer.println(status);
            writer.flush();
        }

        private void renderForm(java.io.PrintWriter writer) {
            writer.println("┌──────────── 添加待办 ────────────┐");
            writer.println("│ " + (focusedField == 0 ? ">" : " ") + " 标题: " + safe(title.toString()));
            writer.println("│ " + (focusedField == 1 ? ">" : " ") + " 描述: " + safe(description.toString()));
            writer.println("│ " + (focusedField == 2 ? ">" : " ") + " [ 确定 ]");
            writer.println("└──────────────────────────────────┘");
            writer.println("Tab 切换, Enter 确认, Esc 返回");
        }

        private String safe(String value) {
            return value.replaceAll("\\p{Cntrl}", " ");
        }

        private String truncate(String value) {
            int maxLength = Math.max(20, terminal.getWidth() - 4);
            int codePoints = value.codePointCount(0, value.length());
            if (codePoints <= maxLength) {
                return value;
            }
            int end = value.offsetByCodePoints(0, maxLength - 3);
            return value.substring(0, end) + "...";
        }
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

        if (!flag.get()) {
            runTui();
            flag.set(true);
        }

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
