package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@CustomLog
public class BookRecord extends BaseRecord {

    private static final int PAGE_SIZE = 10;
    private static final int KEY_UP = 1001;
    private static final int KEY_DOWN = 1002;
    private static final int KEY_LEFT = 1003;
    private static final int KEY_RIGHT = 1004;
    private static final int KEY_ESCAPE = 1005;

    @Override
    public ToolName name() {
        return ToolName.bookRecord;
    }

    @Override
    protected Type currentType() {
        return Type.book;
    }

    @Override
    public void run(Args args) throws Exception {
        var find = args.readArg("find");
        if (find.isPresent()) {
            args.getContext().write(findActions(find));
            return;
        }
        runTui();
    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("find", null, "search condition, use URL query string", false, "category=ebook"))
            .runnerTypes(EnumSet.allOf(RunnerType.class));
    }

    private void runTui() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            new BookBoard(terminal, reader).run();
        }
    }

    private final class BookBoard {

        private final Terminal terminal;
        private final LineReader reader;
        private final List<ActionRecord> books = new ArrayList<>();
        private int page = 1;
        private int selectedIndex;
        private boolean hasNextPage;
        private String query = "";
        private String category = "";
        private String status = "";
        private String error = "";

        private BookBoard(Terminal terminal, LineReader reader) {
            this.terminal = terminal;
            this.reader = reader;
        }

        private void run() throws IOException {
            runAction(this::loadBooks);
            while (true) {
                render();
                int key = readKey();
                if (key == -1 || key == 'q' || key == 'Q' || key == KEY_ESCAPE) {
                    return;
                }
                handleKey(key);
            }
        }

        private void handleKey(int key) throws IOException {
            if (key == KEY_UP && selectedIndex > 0) {
                selectedIndex--;
            } else if (key == KEY_DOWN && selectedIndex < books.size() - 1) {
                selectedIndex++;
            } else if ((key == KEY_LEFT || key == 'p' || key == 'P') && page > 1) {
                page--;
                runAction(this::loadBooks);
            } else if ((key == KEY_RIGHT || key == 'n' || key == 'N') && hasNextPage) {
                page++;
                runAction(this::loadBooks);
            } else if (key == 's' || key == 'S') {
                search();
            } else if (key == 'c' || key == 'C') {
                category = switch (category) {
                    case "" -> "ebook";
                    case "ebook" -> "comic";
                    default -> "";
                };
                page = 1;
                runAction(this::loadBooks);
            } else if (key == 'r' || key == 'R') {
                runAction(this::loadBooks);
            }
        }

        private void search() throws IOException {
            String input = reader.readLine("搜索书名（留空显示全部）: ");
            if (input == null) return;
            query = input.trim();
            page = 1;
            selectedIndex = 0;
            status = query.isEmpty() ? "已清除搜索" : "搜索完成";
            runAction(this::loadBooks);
        }

        private void loadBooks() {
            StringBuilder path = new StringBuilder("/action?type=book&page=")
                .append(page)
                .append("&size=")
                .append(PAGE_SIZE + 1);
            if (!category.isBlank()) {
                path.append("&category=").append(encode(category));
            }
            if (!query.isBlank()) {
                path.append("&name=").append(encode(query));
            }
            Actions result = getRequestLocalData(path.toString(), Actions.class);
            hasNextPage = result.size() > PAGE_SIZE;
            books.clear();
            books.addAll(result.subList(0, Math.min(PAGE_SIZE, result.size())));
            selectedIndex = Math.min(selectedIndex, Math.max(0, books.size() - 1));
        }

        private void render() {
            var writer = terminal.writer();
            writer.print("\u001b[H\u001b[2J");
            writer.println("书籍记录");
            writer.println("↑/↓ 浏览  S 搜索  C 切换类别  ←/P 上页  →/N 下页  R 刷新  Q 退出");
            writer.println("搜索: " + (query.isBlank() ? "（无）" : query)
                + " · 类别: " + (category.isBlank() ? "全部" : category));
            writer.println("─".repeat(Math.max(1, Math.min(terminal.getWidth(), 80))));
            if (books.isEmpty()) {
                writer.println(query.isBlank() ? "还没有书籍记录" : "没有匹配的书籍");
            } else {
                for (int i = 0; i < books.size(); i++) {
                    ActionRecord book = books.get(i);
                    String marker = i == selectedIndex ? "> " : "  ";
                    String labels = StringUtils.isBlank(book.labels()) ? "" : " ." + book.labels();
                    writer.println(marker + truncate(safe(book.name()) + " [" + safe(book.category()) + "]" + labels));
                    if (StringUtils.isNotBlank(book.remark())) {
                        writer.println("    " + truncate(safe(book.remark())));
                    }
                    writer.println("    " + safe(book.state()));
                }
            }
            writer.println();
            writer.println("第 " + page + " 页 · " + books.size() + " 条" + (hasNextPage ? " · 还有下一页" : ""));
            if (!error.isBlank()) writer.println("错误: " + error);
            if (!status.isBlank()) writer.println(status);
            writer.flush();
        }

        private int readKey() throws IOException {
            var attributes = terminal.enterRawMode();
            try {
                int key = terminal.reader().read();
                if (key != 27) return key;
                if (terminal.reader().read(100) != '[') return KEY_ESCAPE;
                return switch (terminal.reader().read(100)) {
                    case 'A' -> KEY_UP;
                    case 'B' -> KEY_DOWN;
                    case 'C' -> KEY_RIGHT;
                    case 'D' -> KEY_LEFT;
                    default -> KEY_ESCAPE;
                };
            } finally {
                terminal.setAttributes(attributes);
            }
        }

        private String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private String safe(String value) {
            return Objects.toString(value, "").replaceAll("\\p{Cntrl}", " ");
        }

        private String truncate(String value) {
            int width = Math.max(20, terminal.getWidth() - 4);
            int count = value.codePointCount(0, value.length());
            if (count <= width) return value;
            return value.substring(0, value.offsetByCodePoints(0, width - 3)) + "...";
        }

        private void runAction(CheckedAction action) {
            try {
                error = "";
                action.run();
            } catch (Exception e) {
                error = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
                log.error("Book record TUI query failed", e);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
