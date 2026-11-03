package net.cofcool.sourcebox.internal;

import lombok.CustomLog;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionState;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.CheckList;
import net.cofcool.sourcebox.internal.api.entity.Habit;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@CustomLog
public class Habits extends BaseRecord {

    private static final int ACTION_PAGE_SIZE = 100;

    @Override
    public ToolName name() {
        return ToolName.habits;
    }

    @Override
    protected Type currentType() {
        return Type.habit;
    }

    @Override
    protected boolean callOthers(Args args) throws Exception {
        var definitionsPath = args.readArg("importDefinitions").optVal();
        var dailyPath = args.readArg("importDaily").optVal();
        if (definitionsPath.isEmpty() && dailyPath.isEmpty()) {
            return false;
        }

        List<ParsedDefinition> definitions = definitionsPath
            .map(this::readDefinitions)
            .orElseGet(List::of);
        List<DailyRecord> dailyRecords = dailyPath
            .map(this::readDailyRecords)
            .orElseGet(List::of);
        Map<String, ActionRecord> actionsByName = loadExistingHabits();
        List<DailyValue> values = validateDailyRecords(dailyRecords, definitions, actionsByName);
        int savedDefinitions = saveDefinitions(definitions, actionsByName);
        int savedValues = saveDailyValues(values, actionsByName);

        args.getContext().write("Imported " + savedDefinitions + " habit definitions and "
            + savedValues + " daily values");
        return true;
    }

    private List<ParsedDefinition> readDefinitions(String path) {
        try {
            return parseDefinitions(Files.readString(Path.of(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read habit definitions CSV: " + path, e);
        }
    }

    private List<DailyRecord> readDailyRecords(String path) {
        try {
            return parseDailyRecords(Files.readString(Path.of(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read habit daily records CSV: " + path, e);
        }
    }

    private Map<String, ActionRecord> loadExistingHabits() {
        Map<String, ActionRecord> result = new LinkedHashMap<>();
        for (int page = 1; ; page++) {
            Actions actions = getRequestLocalData(
                "/action?type=habit&page=" + page + "&size=" + ACTION_PAGE_SIZE,
                Actions.class
            );
            for (ActionRecord action : actions) {
                ActionRecord previous = result.putIfAbsent(action.name(), action);
                if (previous != null) {
                    throw new IllegalStateException("Multiple habits have the same name: " + action.name());
                }
            }
            if (actions.size() < ACTION_PAGE_SIZE) {
                return result;
            }
        }
    }

    static List<DailyValue> validateDailyRecords(
        List<DailyRecord> records,
        List<ParsedDefinition> definitions,
        Map<String, ActionRecord> actionsByName
    ) {
        Set<String> importedNames = new HashSet<>();
        definitions.forEach(definition -> importedNames.add(definition.action().name()));

        List<DailyValue> values = new ArrayList<>();
        for (DailyRecord record : records) {
            for (Map.Entry<String, String> entry : record.values().entrySet()) {
                String name = entry.getKey();
                Integer value = parseDailyValue(entry.getValue());
                if (value == null) {
                    continue;
                }
                if (!importedNames.contains(name) && !actionsByName.containsKey(name)) {
                    throw new IllegalArgumentException("Daily CSV refers to unknown habit '" + name
                        + "' on " + record.date());
                }
                values.add(new DailyValue(name, record.date(), value));
            }
        }
        return values;
    }

    private int saveDefinitions(List<ParsedDefinition> definitions, Map<String, ActionRecord> actionsByName) {
        for (ParsedDefinition definition : definitions) {
            ActionRecord previous = actionsByName.get(definition.action().name());
            ActionRecord action = definition.action();
            if (previous != null) {
                action = action.withId(previous.id())
                    .withStart(previous.start() == null ? action.start() : previous.start());
            }
            ActionRecord saved = postRequestLocalData(
                "/action/habit",
                new Habit.HabitRecord(action, definition.habit()),
                ActionRecord.class
            );
            actionsByName.put(saved.name(), saved);
        }
        return definitions.size();
    }

    private int saveDailyValues(List<DailyValue> values, Map<String, ActionRecord> actionsByName) {
        for (DailyValue value : values) {
            ActionRecord action = actionsByName.get(value.habitName());
            CheckList request = CheckList.builder()
                .current(value.value())
                .createTime(value.date().atStartOfDay())
                .build();
            postRequestLocalData("/action/habit/" + action.id(), request, CheckList.class);
        }
        return values.size();
    }

    static List<ParsedDefinition> parseDefinitions(String content) {
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Habit definitions CSV is empty");
        }
        Map<String, Integer> columns = columnIndexes(rows.getFirst());
        requireColumn(columns, "Name");
        requireColumn(columns, "Type");

        List<ParsedDefinition> definitions = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }
            String name = valueAt(row, columns, "Name").trim();
            if (name.isBlank()) {
                throw rowError(rowIndex, "Name must not be blank");
            }
            if (!names.add(name)) {
                throw rowError(rowIndex, "Duplicate habit name: " + name);
            }

            String category = switch (valueAt(row, columns, "Type").trim().toUpperCase(Locale.ROOT)) {
                case "YES_NO" -> "check";
                case "NUMERICAL" -> "value";
                default -> throw rowError(rowIndex, "Unsupported habit Type: " + valueAt(row, columns, "Type"));
            };
            int freqNum = positiveInt(valueAt(row, columns, "FrequencyNumerator"), 1,
                rowIndex, "FrequencyNumerator");
            int freqDen = positiveInt(valueAt(row, columns, "FrequencyDenominator"), 1,
                rowIndex, "FrequencyDenominator");
            String question = emptyToNull(valueAt(row, columns, "Question"));
            String description = emptyToNull(valueAt(row, columns, "Description"));
            String unit = emptyToNull(valueAt(row, columns, "Unit"));
            Integer targetValue = null;
            String targetType = null;
            if (category.equals("value")) {
                targetValue = integerValue(valueAt(row, columns, "Target Value"), rowIndex, "Target Value");
                targetType = switch (valueAt(row, columns, "Target Type").trim().toUpperCase(Locale.ROOT)) {
                    case "AT_LEAST" -> Habit.TargetType.AtLeast.name();
                    case "AT_MOST" -> Habit.TargetType.AtMost.name();
                    default -> throw rowError(rowIndex, "Unsupported Target Type: "
                        + valueAt(row, columns, "Target Type"));
                };
            }

            boolean archived = parseBoolean(valueAt(row, columns, "Archived?"), rowIndex);
            LocalDateTime now = LocalDateTime.now();
            ActionRecord action = ActionRecord.builder()
                .name(name)
                .icon(emptyToNull(valueAt(row, columns, "Color")))
                .index(emptyToNull(valueAt(row, columns, "Position")))
                .type(Type.habit.name())
                .category(category)
                .state(archived ? ActionState.cancel.name() : ActionState.doing.name())
                .remark(description)
                .start(now)
                .build();
            Habit habit = Habit.builder()
                .category(category)
                .note(description)
                .unit(unit)
                .freqNum(freqNum)
                .freqDen(freqDen)
                .question(question)
                .targetValue(targetValue)
                .targetType(targetType)
                .build();
            definitions.add(new ParsedDefinition(action, habit));
        }
        return definitions;
    }

    static List<DailyRecord> parseDailyRecords(String content) {
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Habit daily records CSV is empty");
        }
        Map<String, Integer> columns = columnIndexes(rows.getFirst());
        requireColumn(columns, "Date");

        List<DailyRecord> records = new ArrayList<>();
        Set<LocalDate> dates = new HashSet<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (isBlankRow(row)) {
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(valueAt(row, columns, "Date").trim());
            } catch (DateTimeParseException e) {
                throw rowError(rowIndex, "Date must use yyyy-MM-dd");
            }
            if (!dates.add(date)) {
                throw rowError(rowIndex, "Duplicate date: " + date);
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> column : columns.entrySet()) {
                if (column.getKey().equalsIgnoreCase("Date")) {
                    continue;
                }
                String value = row.size() > column.getValue() ? row.get(column.getValue()).trim() : "";
                if (!value.isEmpty()) {
                    values.put(column.getKey(), value);
                }
            }
            records.add(new DailyRecord(date, values));
        }
        return records;
    }

    static Integer parseDailyValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "", "UNKNOWN", "SKIP", "YES_AUTO" -> {
                return null;
            }
            case "YES_MANUAL" -> {
                return 1;
            }
            case "NO" -> {
                return 0;
            }
        }
        try {
            return new BigDecimal(raw.trim()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Habit value must be a check marker or integer: " + raw, e);
        }
    }

    private static int positiveInt(String value, int defaultValue, int row, String column) {
        if (value.isBlank()) {
            return defaultValue;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw rowError(row, column + " must be a positive integer");
        }
        if (parsed < 1) {
            throw rowError(row, column + " must be a positive integer");
        }
        return parsed;
    }

    private static int integerValue(String value, int row, String column) {
        try {
            return new BigDecimal(value.trim()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw rowError(row, column + " must be an integer");
        }
    }

    private static boolean parseBoolean(String value, int row) {
        if (value.isBlank() || value.equalsIgnoreCase("false")) {
            return false;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        throw rowError(row, "Archived? must be true or false");
    }

    private static Map<String, Integer> columnIndexes(List<String> header) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Set<String> normalizedNames = new HashSet<>();
        for (int index = 0; index < header.size(); index++) {
            String name = header.get(index).trim();
            if (name.isBlank()) {
                continue;
            }
            if (!normalizedNames.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate CSV column: " + name);
            }
            result.put(name, index);
        }
        return result;
    }

    private static void requireColumn(Map<String, Integer> columns, String column) {
        if (findColumn(columns, column) == null) {
            throw new IllegalArgumentException("Missing CSV column: " + column);
        }
    }

    private static String valueAt(List<String> row, Map<String, Integer> columns, String column) {
        Integer index = findColumn(columns, column);
        return index == null || index >= row.size() ? "" : row.get(index);
    }

    private static Integer findColumn(Map<String, Integer> columns, String name) {
        return columns.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(String::isBlank);
    }

    private static IllegalArgumentException rowError(int rowIndex, String message) {
        return new IllegalArgumentException("CSV row " + (rowIndex + 1) + ": " + message);
    }

    private static List<List<String>> parseCsv(String content) {
        String csv = content.startsWith("\uFEFF") ? content.substring(1) : content;
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean afterQuote = false;

        for (int i = 0; i < csv.length(); i++) {
            char current = csv.charAt(i);
            if (inQuotes) {
                if (current == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                        afterQuote = true;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == '"') {
                if (field.isEmpty() && !afterQuote) {
                    inQuotes = true;
                } else {
                    throw new IllegalArgumentException("Unexpected quote in CSV");
                }
            } else if (current == ',') {
                row.add(field.toString());
                field.setLength(0);
                afterQuote = false;
            } else if (current == '\n' || current == '\r') {
                row.add(field.toString());
                addCsvRow(rows, row);
                row = new ArrayList<>();
                field.setLength(0);
                afterQuote = false;
                if (current == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (afterQuote && !Character.isWhitespace(current)) {
                throw new IllegalArgumentException("Unexpected character after quoted CSV field");
            } else if (!afterQuote) {
                field.append(current);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quoted CSV field");
        }
        if (!row.isEmpty() || !field.isEmpty()) {
            row.add(field.toString());
            addCsvRow(rows, row);
        }
        return rows;
    }

    private static void addCsvRow(List<List<String>> rows, List<String> row) {
        if (!isBlankRow(row)) {
            rows.add(List.copyOf(row));
        }
    }

    record ParsedDefinition(ActionRecord action, Habit habit) {
    }

    record DailyRecord(LocalDate date, Map<String, String> values) {
    }

    record DailyValue(String habitName, LocalDate date, int value) {
    }

    @Override
    public Args config() {
        return super.config()
            .arg(new Arg("importDefinitions", null,
                "CSV file containing habit definitions", false, "./habits.csv"))
            .arg(new Arg("importDaily", null,
                "CSV file containing daily habit values", false, "./habit-records.csv"));
    }
}
