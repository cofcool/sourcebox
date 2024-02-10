package net.cofcool.toolbox.util;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.HashMap;

public class LogseqOutStr {

    public static DateTimeFormatter DATE_FORMATTER;

    static {
        var day = new HashMap<Long, String>();
        for (int i = 0; i < 31; i++) {
            var val = i + switch (i) {
                case 1, 21 ->  "st";
                case 2, 22 ->  "nd";
                case 3, 23 ->  "rd";
                default -> "th";
            };
            day.put((long) i, val);
        }
        var moy = new HashMap<Long, String>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendText(MONTH_OF_YEAR, moy)
            .appendLiteral(' ')
            .appendText(DAY_OF_MONTH, day)
            .appendLiteral(", ")
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .toFormatter();
    }
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss");
    public static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(
            new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 0, 9, true)
                .optionalStart()
                .appendFraction(MILLI_OF_SECOND, 0, 3, true)
                .appendLiteral("Z")
                .toFormatter()
        )
        .toFormatter();
    private final StringBuilder builder = new StringBuilder();

    public LogseqOutStr append(String val) {
        if (val != null) {
            builder.append(val);
        }
        return this;
    }

    public LogseqOutStr blockRef(String val) {
        if (val != null && !val.isEmpty()) {
            if (builder.lastIndexOf(" ") != (builder.length() - 1)) {
                builder.append(" ");
            }
            builder.append("[[").append(val).append("]]");
        }
        return this;
    }

    public LogseqOutStr tag(String val) {
        if (val != null && !val.isEmpty()) {
            builder.append(" #").append(val);
        }
        return this;
    }

    public static String cardTask(String val) {
        return switch (val) {
            case "now", "reading", "watching", "doing", "often" -> "DOING ";
            case "waiting" -> "TODO ";
            default -> "DONE ";
        };
    }

    public static String sateTask(String val) {
        return "complete".equals(val) ? "DONE " : "TODO ";
    }

    public static String date(LocalDateTime val) {
        return DATE_FORMATTER.format(val);
    }

    public static String date(String val) {
        return DATE_FORMATTER.format(LocalDateTime.parse(val, FORMATTER));
    }

    public static String time(String val) {
        return TIME_FORMATTER.format(LocalDateTime.parse(val, FORMATTER));
    }

    public LogseqOutStr breakLine() {
        builder.append("\n");
        return this;
    }

    public LogseqOutStr block(String val, int tab) {
        if (!val.isBlank()) {
            builder.append(tab == 0 ? "" : ("\t".repeat(Math.max(0, tab)))).append("- ").append(val);
        }
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
