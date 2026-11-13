package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.WebTool;
import net.cofcool.sourcebox.internal.api.entity.ListData;
import net.cofcool.sourcebox.internal.api.entity.Note;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@CustomLog
public class SimpleNote implements WebTool {

    private static final int KEY_UP = 1001;
    private static final int KEY_DOWN = 1002;
    private static final int KEY_ESCAPE = 1003;

    @Override
    public ToolName name() {
        return ToolName.note;
    }

    @Override
    public void run(Args args) throws Exception {
        runTui();
    }

    private void runTui() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            NoteBoard board = new NoteBoard(terminal, reader);
            try {
                terminal.writer().print("\u001b[?25l");
                terminal.writer().flush();
                board.run();
            } finally {
                terminal.writer().print("\u001b[0m\u001b[?25h");
                terminal.writer().flush();
            }
        }
    }

    private Notes loadNotes(String query) {
        if (StringUtils.isBlank(query)) {
            return getRequestLocalData("/note/list", Notes.class);
        }
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        SearchResults results = getRequestLocalData(
            "/search?q=" + encodedQuery + "&type=note",
            SearchResults.class
        );
        Notes notes = new Notes();
        results.stream()
            .filter(result -> result.data() != null)
            .map(SearchResult::data)
            .forEach(notes::add);
        return notes;
    }

    private final class NoteBoard {

        private final Terminal terminal;
        private final LineReader reader;
        private Notes notes = new Notes();
        private int selectedIndex;
        private Note selected;
        private String query = "";
        private String status = "";
        private String error = "";

        private NoteBoard(Terminal terminal, LineReader reader) {
            this.terminal = terminal;
            this.reader = reader;
        }

        private void run() throws IOException {
            runAction(this::refresh);
            while (true) {
                render();
                int key = readKey();
                if (key == -1 || key == 'q' || key == 'Q') {
                    return;
                }
                handleKey(key);
            }
        }

        private void handleKey(int key) throws IOException {
            if (key == KEY_UP && selectedIndex > 0) {
                selectedIndex--;
            } else if (key == KEY_DOWN && selectedIndex < notes.size() - 1) {
                selectedIndex++;
            } else if (key == '\r' || key == '\n') {
                selectNote();
            } else if (key == 'a' || key == 'A' || key == 'n' || key == 'N') {
                createNote();
            } else if (key == 'e' || key == 'E') {
                editNote();
            } else if (key == 'd' || key == 'D') {
                deleteNote();
            } else if (key == 's' || key == 'S') {
                searchNotes();
            } else if (key == 'r' || key == 'R') {
                runAction(this::refresh);
            } else if (key == KEY_ESCAPE) {
                selected = null;
                status = "";
            }
        }

        private void selectNote() {
            if (notes.isEmpty()) {
                return;
            }
            selected = notes.get(selectedIndex);
            status = "便签已载入，按 E 编辑或按 D 删除";
        }

        private void createNote() throws IOException {
            String content = editContent("");
            if (content == null) {
                status = "已取消新建";
                return;
            }
            if (StringUtils.isBlank(content)) {
                status = "便签内容不能为空";
                return;
            }
            save(null, content);
        }

        private void editNote() throws IOException {
            if (selected == null) {
                status = "请先从列表选择一条便签";
                return;
            }
            String content = editContent(selected.content());
            if (content == null) {
                status = "已取消编辑";
                return;
            }
            if (StringUtils.isBlank(content)) {
                status = "便签内容不能为空";
                return;
            }
            save(selected.id(), content);
        }

        private String editContent(String currentContent) throws IOException {
            terminal.writer().print("\u001b[H\u001b[2J");
            terminal.writer().println("便签编辑器");
            terminal.writer().println("当前内容:");
            terminal.writer().println(safe(currentContent));
            terminal.writer().println();
            terminal.writer().println("输入新的内容；单独输入 . 保存，单独输入 \\. 表示一个句点行。");
            terminal.writer().println("Ctrl+D 取消。");
            terminal.writer().flush();

            var content = new StringBuilder();
            while (true) {
                String line;
                try {
                    line = reader.readLine(content.isEmpty() ? "> " : "  ");
                } catch (EndOfFileException e) {
                    return null;
                }
                if (".".equals(line)) {
                    return content.toString();
                }
                if ("\\.".equals(line)) {
                    line = ".";
                }
                if (!content.isEmpty()) {
                    content.append('\n');
                }
                content.append(line);
            }
        }

        private void save(String id, String content) {
            Note saved = postRequestLocalData(
                "/note/note",
                Note.builder()
                    .id(id)
                    .content(content.trim())
                    .date(java.time.LocalDateTime.now())
                    .state(Note.NoteState.NORMAL.name())
                    .build(),
                Note.class
            );
            query = "";
            selected = saved;
            status = id == null ? "便签已添加" : "便签已保存";
            refresh();
            selected = notes.stream().filter(note -> note.id().equals(saved.id())).findFirst().orElse(saved);
            selectedIndex = notes.indexOf(selected);
        }

        private void deleteNote() throws IOException {
            if (selected == null) {
                status = "请先从列表选择一条便签";
                return;
            }
            String confirmation = reader.readLine("删除“" + truncate(selected.content()) + "”？输入 y 确认: ");
            if (!"y".equalsIgnoreCase(confirmation)) {
                status = "已取消删除";
                return;
            }
            if (!deleteRequestLocalData("/note/" + URLEncoder.encode(selected.id(), StandardCharsets.UTF_8))) {
                throw new IllegalStateException("删除便签失败");
            }
            selected = null;
            status = "便签已删除";
            refresh();
        }

        private void searchNotes() throws IOException {
            String input = reader.readLine("搜索便签（留空显示全部）: ");
            if (input == null) {
                return;
            }
            query = input.trim();
            selected = null;
            selectedIndex = 0;
            runAction(this::refresh);
            status = query.isEmpty() ? "已清除搜索" : "搜索完成";
        }

        private void refresh() {
            notes = loadNotes(query);
            selectedIndex = Math.min(selectedIndex, Math.max(0, notes.size() - 1));
            if (selected != null) {
                selected = notes.stream()
                    .filter(note -> note.id().equals(selected.id()))
                    .findFirst()
                    .orElse(null);
            }
        }

        private int readKey() throws IOException {
            Attributes attributes = terminal.enterRawMode();
            try {
                int key = terminal.reader().read();
                if (key != 27) {
                    return key;
                }
                int next = terminal.reader().read(100);
                if (next != '[') {
                    return KEY_ESCAPE;
                }
                return switch (terminal.reader().read(100)) {
                    case 'A' -> KEY_UP;
                    case 'B' -> KEY_DOWN;
                    default -> KEY_ESCAPE;
                };
            } finally {
                terminal.setAttributes(attributes);
            }
        }

        private void render() {
            var writer = terminal.writer();
            writer.print("\u001b[H\u001b[2J");
            writer.println("便签");
            writer.println("↑/↓ 选择  Enter 查看  A 新建  E 编辑  D 删除  S 搜索  R 刷新  q 退出");
            writer.println("搜索: " + (query.isBlank() ? "（无）" : query));
            writer.println("─".repeat(Math.max(1, Math.min(terminal.getWidth(), 72))));
            if (notes.isEmpty()) {
                writer.println(query.isBlank() ? "没有笔记，在上方新建第一条便签" : "没有匹配便签，试试其他关键词");
            } else {
                int visibleRows = Math.max(1, (terminal.getHeight() - 8) / 3);
                int firstVisible = Math.max(0, Math.min(
                    selectedIndex - visibleRows + 1,
                    notes.size() - visibleRows
                ));
                int lastVisible = Math.min(notes.size(), firstVisible + visibleRows);
                for (int i = firstVisible; i < lastVisible; i++) {
                    Note note = notes.get(i);
                    writer.println((i == selectedIndex ? "> " : "  ")
                        + Objects.toString(note.date(), "未设置时间"));
                    writer.println("  " + truncate(safe(note.content())));
                    writer.println();
                }
            }
            if (selected != null) {
                writer.println("内容:");
                int remainingRows = Math.max(1, terminal.getHeight() - 10);
                String[] lines = selected.content().split("\\R", -1);
                for (int i = 0; i < Math.min(lines.length, remainingRows); i++) {
                    writer.println("  " + truncate(safe(lines[i])));
                }
                if (lines.length > remainingRows) {
                    writer.println("  ...");
                }
            }
            if (!error.isBlank()) {
                writer.println("错误: " + error);
            }
            if (!status.isBlank()) {
                writer.println(status);
            }
            writer.flush();
        }

        private String truncate(String value) {
            int width = Math.max(20, terminal.getWidth() - 4);
            int codePoints = value.codePointCount(0, value.length());
            return codePoints <= width ? value
                : value.substring(0, value.offsetByCodePoints(0, width - 3)) + "...";
        }

        private String safe(String value) {
            return StringUtils.defaultString(value).replaceAll("\\p{Cntrl}", " ");
        }

        private void runAction(CheckedAction action) {
            try {
                error = "";
                action.run();
            } catch (Exception e) {
                error = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                log.error("Note TUI action failed", e);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    public static class Notes extends ListData<Note> {
    }

    public static class SearchResults extends ListData<SearchResult> {
    }

    public record SearchResult(String type, String id, String title, float score, Note data) {
    }

    @Override
    public Args config() {
        return new Args();
    }

}
