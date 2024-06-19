package net.cofcool.sourcebox.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DataSize(double size) {

    public DataSize(double size, Unit unit) {
        this(size * unit.step);
    }

    public double to(Unit unit) {
        return size / unit.step;
    }

    @Override
    public String toString() {
        if ((long)to(Unit.TB) > 0) {
            return String.format("%.2f TB", toGB());
        } else if ((long)toGB() > 0) {
            return String.format("%.2f GB", toGB());
        } else if ((long)toMB() > 0) {
            return String.format("%.2f MB", toMB());
        } else {
            return String.format("%.2f KB", toKB());
        }
    }

    public double toGB() {
        return to(Unit.GB);
    }
    public double toMB() {
        return to(Unit.MB);
    }
    public double toKB() {
        return to(Unit.KB);
    }

    /**
     * 1kb/k, 1mb/m, 1gb/g, 不区分大小写
     */
    public static DataSize of(String value) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(B|KB|K|M|MB|G|GB|T|TB|P|PB)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(STR."Invalid format: \{value}");
        }

        double size = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(3).toUpperCase();
        return switch (unit) {
            case "B" -> new DataSize(size);
            case "K", "KB" -> new DataSize(size, Unit.KB);
            case "M", "MB" -> new DataSize(size, Unit.MB);
            case "G", "GB" -> new DataSize(size, Unit.GB);
            case "T", "TB" -> new DataSize(size, Unit.TB);
            case "P", "PB" -> new DataSize(size, Unit.PB);
            default -> throw new IllegalArgumentException(STR."Unknown unit: \{unit}");
        };
    }

    public enum Unit {
        B(1),
        KB(1024),
        MB(Math.pow(1024, 2)),
        GB(Math.pow(1024, 3)),
        TB(Math.pow(1024, 4)),
        PB(Math.pow(1024, 5));

        private final double step;

        Unit(double step) {
            this.step = step;
        }
    }
}
