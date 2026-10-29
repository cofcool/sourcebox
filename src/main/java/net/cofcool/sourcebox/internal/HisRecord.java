package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.*;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CustomLog
public class HisRecord extends BaseRecord {

    private static final int PAGE_SIZE = 10;
    private static final int KEY_UP = 1001;
    private static final int KEY_DOWN = 1002;
    private static final int KEY_LEFT = 1003;
    private static final int KEY_RIGHT = 1004;
    private static final int KEY_ESCAPE = 1005;
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ToolName name() {
        return ToolName.hisRecord;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
        runTui();
        return true;
    }

    private boolean shouldLaunchTui(Args args) {
        return args.getContext() != null
            && args.isCurrentType(RunnerType.CLI)
            && System.console() != null
            && !args.readArg("find").isPresent()
            && !args.readArg("add").isPresent()
            && !args.readArg("done").isPresent()
            && !args.readArg("cancel").isPresent()
            && !args.readArg("delete").isPresent();
    }

    private void runTui() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            RecordBoard board = new RecordBoard(terminal, reader);
            board.run();
        }
    }

    @Override
    protected Type currentType() {
        return Type.record;
    }

    @Override
    public Args config() {
        return super.config();
    }

    public static class SearchResults extends ListData<SearchResult> {
    }

    public record SearchResult(String type, String id, String title, float score, ActionRecord data) {
    }

    private final class RecordBoard {

        private final Terminal terminal;
        private final LineReader reader;
        private final List<ActionRecord> records = new ArrayList<>();
        private final List<String> categories = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();
        private int page = 1;
        private int selectedIndex;
        private boolean hasNextPage;
        private String query = "";
        private String status = "";
        private String error = "";
        private ActionRecord selected;
        private List<Comment> comments = List.of();
        private List<CheckList> checklists = List.of();

        private RecordBoard(Terminal terminal, LineReader reader) {
            this.terminal = terminal;
            this.reader = reader;
        }

        private void run() throws IOException {
            runAction(this::loadTypeOptions);
            runAction(this::loadRecords);
            while (true) {
                if (selected == null) {
                    renderList();
                    int key = readKey();
                    if (key == -1 || key == 'q' || key == 'Q') {
                        return;
                    }
                    handleListKey(key);
                } else {
                    renderDetail();
                    int key = readKey();
                    if (key == -1 || key == KEY_ESCAPE) {
                        selected = null;
                        status = "";
                    } else {
                        handleDetailKey(key);
                    }
                }
            }
        }

        private void handleListKey(int key) throws IOException {
            if (key == KEY_UP && selectedIndex > 0) {
                selectedIndex--;
            } else if (key == KEY_DOWN && selectedIndex < records.size() - 1) {
                selectedIndex++;
            } else if (key == KEY_LEFT || key == 'p' || key == 'P') {
                if (page > 1) {
                    page--;
                    runAction(this::loadRecords);
                }
            } else if (key == KEY_RIGHT || key == 'n' || key == 'N') {
                if (hasNextPage) {
                    page++;
                    runAction(this::loadRecords);
                }
            } else if (key == '\r' || key == '\n') {
                if (!records.isEmpty()) {
                    runAction(() -> openRecord(records.get(selectedIndex)));
                }
            } else if (key == 'a' || key == 'A') {
                runAction(this::addRecord);
            } else if (key == 's' || key == 'S') {
                searchRecords();
            } else if (key == 'r' || key == 'R') {
                runAction(this::loadRecords);
            }
        }

        private void handleDetailKey(int key) throws IOException {
            if (key == 'e' || key == 'E') {
                runAction(this::editRecord);
            } else if (key == 'c' || key == 'C') {
                runAction(this::editChecklists);
            } else if (key == 'r' || key == 'R') {
                runAction(this::addReply);
            }
        }

        private void loadRecords() {
            List<ActionRecord> matches;
            if (StringUtils.isBlank(query)) {
                matches = getRequestLocalData(
                    "/action?type=record&page=" + page + "&size=" + (PAGE_SIZE + 1),
                    Actions.class
                );
            } else {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                SearchResults results = getRequestLocalData(
                    "/search?q=" + encodedQuery + "&type=action",
                    SearchResults.class
                );
                matches = results.stream()
                    .filter(result -> result.data() != null && Type.record.name().equals(result.data().type()))
                    .map(SearchResult::data)
                    .skip((long) (page - 1) * PAGE_SIZE)
                    .limit(PAGE_SIZE + 1L)
                    .toList();
            }
            hasNextPage = matches.size() > PAGE_SIZE;
            records.clear();
            records.addAll(matches.subList(0, Math.min(matches.size(), PAGE_SIZE)));
            selectedIndex = Math.min(selectedIndex, Math.max(0, records.size() - 1));
        }

        private void loadTypeOptions() {
            categories.clear();
            labels.clear();
            categories.addAll(getRequestLocalData("/action/types?type=category", ActionTypes.class)
                .stream().map(type -> type.name()).toList());
            labels.addAll(getRequestLocalData("/action/types?type=label", ActionTypes.class)
                .stream().map(type -> type.name()).toList());
        }

        private void addRecord() throws IOException {
            String name = prompt("卡片名称");
            if (name == null || StringUtils.isBlank(name)) {
                status = "已取消添加";
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            ActionRecord record = ActionRecord.builder()
                .name(name.trim())
                .type(Type.record.name())
                .state("todo")
                .start(now)
                .duration(0)
                .build();
            updateState(record);
            status = "卡片已添加";
            loadRecords();
        }

        private void searchRecords() throws IOException {
            String input = prompt("搜索卡片、备注、标签或回复（留空显示全部）");
            if (input == null) {
                return;
            }
            query = input.trim();
            page = 1;
            selectedIndex = 0;
            status = query.isEmpty() ? "已清除搜索" : "搜索完成";
            runAction(this::loadRecords);
        }

        private void openRecord(ActionRecord record) {
            ActionRecord.RecordRet detail = getRequestLocalData(
                "/action/" + encodePath(record.id()),
                ActionRecord.RecordRet.class
            );
            selected = detail.record();
            comments = detail.comments() == null ? List.of() : detail.comments();
            checklists = detail.checklists() == null ? List.of() : detail.checklists();
            status = "";
            error = "";
        }

        private void editRecord() throws IOException {
            String name = prompt("卡片名称", selected.name());
            if (name == null) return;
            if (StringUtils.isBlank(name)) {
                status = "卡片名称不能为空";
                return;
            }
            String remark = promptOptional("备注", selected.remark());
            if (remark == null) return;
            String tagString = prompt("标签（逗号分隔；已有值: " + String.join(", ", labels) + "）",
                Objects.toString(selected.labels(), ""));
            if (tagString == null) return;
            String category = prompt("类别（可选已有值: " + String.join(", ", categories) + "）",
                Objects.toString(selected.category(), ""));
            if (category == null) return;
            String startText = prompt("开始时间 yyyy-MM-dd HH:mm:ss", format(selected.start()));
            if (startText == null) return;
            String endText = promptOptional("结束时间 yyyy-MM-dd HH:mm:ss", format(selected.end()));
            if (endText == null) return;
            String durationText = prompt("持续时长（分钟）", selected.duration() == null
                ? "" : String.valueOf(Math.round(selected.duration() / 60.0)));
            if (durationText == null) return;

            try {
                LocalDateTime start = parseDateTime(startText);
                LocalDateTime end = StringUtils.isBlank(endText) ? null : parseDateTime(endText);
                Integer duration = StringUtils.isBlank(durationText) ? null
                    : Math.multiplyExact(Integer.parseInt(durationText.trim()), 60);
                if (duration != null && duration < 0) {
                    throw new IllegalArgumentException("持续时长不能为负数");
                }
                selected = updateState(selected
                    .withName(name.trim())
                    .withRemark(StringUtils.isBlank(remark) ? null : remark)
                    .withLabels(StringUtils.isBlank(tagString) ? null : tagString.trim())
                    .withCategory(StringUtils.isBlank(category) ? null : category.trim())
                    .withStart(start)
                    .withEnd(end)
                    .withDuration(duration));
                runAction(this::loadRecords);
                status = "卡片信息已保存";
            } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
                status = "输入格式无效: " + e.getMessage();
            }
        }

        private void addReply() throws IOException {
            String content = prompt("回复内容");
            if (content == null || StringUtils.isBlank(content)) {
                status = "回复未添加";
                return;
            }
            Comment saved = postRequestLocalData(
                "/action/comment/" + encodePath(selected.id()),
                Comment.builder().content(content.trim()).build(),
                Comment.class
            );
            List<Comment> updated = new ArrayList<>(comments);
            updated.add(saved);
            comments = updated;
            status = "回复已添加";
        }

        private void editChecklists() throws IOException {
            int checklistIndex = 0;
            while (true) {
                renderChecklists(checklistIndex);
                int key = readKey();
                if (key == -1 || key == KEY_ESCAPE) {
                    return;
                }
                if (key == KEY_UP && checklistIndex > 0) {
                    checklistIndex--;
                } else if (key == KEY_DOWN && checklistIndex < checklists.size() - 1) {
                    checklistIndex++;
                } else if (key == 'a' || key == 'A') {
                    runAction(this::addChecklist);
                    checklistIndex = Math.max(0, checklists.size() - 1);
                } else if ((key == 'e' || key == 'E') && !checklists.isEmpty()) {
                    int selectedChecklist = checklistIndex;
                    runAction(() -> updateChecklistProgress(selectedChecklist));
                } else if ((key == 'd' || key == 'D') && !checklists.isEmpty()) {
                    int selectedChecklist = checklistIndex;
                    runAction(() -> deleteChecklist(selectedChecklist));
                    checklistIndex = Math.min(checklistIndex, Math.max(0, checklists.size() - 1));
                }
            }
        }

        private void addChecklist() throws IOException {
            String content = prompt("Checklist 名称");
            if (content == null || StringUtils.isBlank(content)) {
                status = "Checklist 未添加";
                return;
            }
            String firstValue = prompt("开始", "0");
            if (firstValue == null) return;
            String lastValue = prompt("结束");
            if (lastValue == null) return;
            int first = Integer.parseInt(firstValue.trim());
            int last = Integer.parseInt(lastValue.trim());
            if (first > last) {
                throw new IllegalArgumentException("开始值不能大于结束值");
            }
            CheckList saved = postRequestLocalData(
                "/action/checklist/" + encodePath(selected.id()),
                CheckList.builder().content(content.trim()).first(first).last(last).current(first).build(),
                CheckList.class
            );
            List<CheckList> updated = new ArrayList<>(checklists);
            updated.add(saved);
            checklists = updated;
            status = "Checklist 已添加";
        }

        private void updateChecklistProgress(int index) throws IOException {
            CheckList checklist = checklists.get(index);
            String currentValue = prompt("“" + checklist.content() + "”当前进度（"
                + checklist.first() + " - " + checklist.last() + "）", String.valueOf(checklist.current()));
            if (currentValue == null) return;
            int current = Integer.parseInt(currentValue.trim());
            if (current < checklist.first() || current > checklist.last()) {
                throw new IllegalArgumentException("当前值必须在开始和结束范围内");
            }
            CheckList saved = postRequestLocalData(
                "/action/checklist/" + encodePath(selected.id()),
                CheckList.builder()
                    .id(checklist.id())
                    .actionId(checklist.actionId())
                    .content(checklist.content())
                    .first(checklist.first())
                    .last(checklist.last())
                    .current(current)
                    .createTime(checklist.createTime())
                    .updateTime(checklist.updateTime())
                    .build(),
                CheckList.class
            );
            List<CheckList> updated = new ArrayList<>(checklists);
            updated.set(index, saved);
            checklists = updated;
            status = "Checklist 进度已保存";
        }

        private void deleteChecklist(int index) {
            CheckList checklist = checklists.get(index);
            if (!deleteRequestLocalData("/action/checklist/" + encodePath(checklist.id()))) {
                throw new IllegalStateException("删除 Checklist 失败");
            }
            List<CheckList> updated = new ArrayList<>(checklists);
            updated.remove(index);
            checklists = updated;
            status = "Checklist 已删除";
        }

        private void renderList() {
            var writer = terminal.writer();
            clearScreen();
            writer.println("历史记录");
            writer.println("↑/↓ 选择  Enter 打开  A 添加  S 搜索  ←/P 上页  →/N 下页  R 刷新  Q 退出");
            writer.println("搜索: " + (query.isBlank() ? "（无）" : query));
            writer.println("─".repeat(Math.max(1, Math.min(terminal.getWidth(), 80))));
            if (records.isEmpty()) {
                writer.println(query.isBlank() ? "还没有卡片" : "没有匹配的卡片");
            } else {
                for (int i = 0; i < records.size(); i++) {
                    ActionRecord record = records.get(i);
                    String marker = i == selectedIndex ? "> " : "  ";
                    String category = StringUtils.isBlank(record.category()) ? "" : " [" + record.category() + "]";
                    String tags = StringUtils.isBlank(record.labels()) ? "" : " #" + record.labels();
                    writer.println(marker + truncate(safe(record.name()) + category + tags));
                    if (StringUtils.isNotBlank(record.remark())) {
                        writer.println("    " + truncate(safe(record.remark())));
                    }
                    writer.println("    " + (record.start() == null ? "未设置日期" : DATE_TIME_FORMAT.format(record.start())));
                }
            }
            writer.println();
            writer.println("第 " + page + " 页 · " + records.size() + " 条" + (hasNextPage ? " · 还有下一页" : ""));
            printMessages(writer);
            writer.flush();
        }

        private void renderDetail() {
            var writer = terminal.writer();
            clearScreen();
            writer.println("历史记录 / 卡片详情");
            writer.println("Esc 返回  E 编辑卡片  C 管理 Checklist  R 添加回复");
            writer.println("─".repeat(Math.max(1, Math.min(terminal.getWidth(), 80))));
            writer.println("名称: " + safe(selected.name()));
            writer.println("备注: " + safe(Objects.toString(selected.remark(), "")));
            writer.println("标签: " + safe(Objects.toString(selected.labels(), "")));
            writer.println("类别: " + safe(Objects.toString(selected.category(), "")));
            writer.println("开始: " + Objects.toString(format(selected.start()), ""));
            writer.println("结束: " + Objects.toString(format(selected.end()), ""));
            writer.println("时长: " + formatDuration(selected.duration()));
            writer.println();
            writer.println("Checklist (" + checklists.size() + ")");
            if (checklists.isEmpty()) {
                writer.println("  还没有 Checklist");
            } else {
                checklists.stream().limit(5).forEach(item -> writer.println("  "
                    + (item.current().equals(item.last()) ? "[已完成] " : "[进行中] ")
                    + safe(item.content()) + " " + item.current() + "/" + item.last()));
                if (checklists.size() > 5) writer.println("  ...");
            }
            writer.println();
            writer.println("回复 (" + comments.size() + ")");
            if (comments.isEmpty()) {
                writer.println("  还没有回复");
            } else {
                comments.stream().skip(Math.max(0, comments.size() - 5L)).forEach(comment -> writer.println(
                    "  " + safe(comment.content()) + "  " + Objects.toString(format(comment.createTime()), "")
                ));
            }
            writer.println();
            printMessages(writer);
            writer.flush();
        }

        private void renderChecklists(int selectedChecklist) {
            var writer = terminal.writer();
            clearScreen();
            writer.println("Checklist · " + safe(selected.name()));
            writer.println("↑/↓ 选择  A 添加  E 更新进度  D 删除  Esc 返回");
            writer.println("─".repeat(Math.max(1, Math.min(terminal.getWidth(), 80))));
            if (checklists.isEmpty()) {
                writer.println("还没有 Checklist");
            } else {
                for (int i = 0; i < checklists.size(); i++) {
                    CheckList item = checklists.get(i);
                    writer.println((i == selectedChecklist ? "> " : "  ")
                        + (item.current().equals(item.last()) ? "[已完成] " : "[进行中] ")
                        + safe(item.content()) + "  " + item.current() + "/" + item.last()
                        + "（" + item.first() + " - " + item.last() + "）");
                }
            }
            writer.println();
            printMessages(writer);
            writer.flush();
        }

        private String prompt(String label) throws IOException {
            return prompt(label, "");
        }

        private String prompt(String label, String current) throws IOException {
            terminal.writer().println();
            String input = reader.readLine(label + (current.isEmpty() ? ": " : " [" + current + "]: "));
            return input == null ? null : input.isEmpty() ? current : input;
        }

        private String promptOptional(String label, String current) throws IOException {
            String input = prompt(label + "（输入 - 清空）", current);
            return "-".equals(input) ? "" : input;
        }

        private int readKey() throws IOException {
            var attributes = terminal.enterRawMode();
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
                    case 'C' -> KEY_RIGHT;
                    case 'D' -> KEY_LEFT;
                    default -> KEY_ESCAPE;
                };
            } finally {
                terminal.setAttributes(attributes);
            }
        }

        private void clearScreen() {
            terminal.writer().print("\u001b[H\u001b[2J");
        }

        private void printMessages(java.io.PrintWriter writer) {
            if (!error.isBlank()) writer.println("错误: " + error);
            if (!status.isBlank()) writer.println(status);
        }

        private String truncate(String value) {
            int width = Math.max(20, terminal.getWidth() - 4);
            int count = value.codePointCount(0, value.length());
            if (count <= width) return value;
            return value.substring(0, value.offsetByCodePoints(0, width - 3)) + "...";
        }

        private String safe(String value) {
            return Objects.toString(value, "").replaceAll("\\p{Cntrl}", " ");
        }

        private String format(LocalDateTime value) {
            return value == null ? "" : DATE_TIME_FORMAT.format(value);
        }

        private String formatDuration(Integer seconds) {
            if (seconds == null) return "未设置";
            int totalMinutes = Math.round(seconds / 60.0f);
            return totalMinutes / 60 + "小时 " + totalMinutes % 60 + "分钟";
        }

        private LocalDateTime parseDateTime(String value) {
            String trimmed = value.trim();
            try {
                return LocalDateTime.parse(trimmed, DATE_TIME_FORMAT);
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.parse(trimmed);
            }
        }

        private String encodePath(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private void runAction(CheckedAction action) {
            try {
                error = "";
                action.run();
            } catch (Exception e) {
                error = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
                log.error("History record TUI action failed", e);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    public static class ActionTypes extends ListData<ActionType> {
    }
}
