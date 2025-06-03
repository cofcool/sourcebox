package net.cofcool.sourcebox.util;

import java.util.ArrayList;
import java.util.List;

public final class CsvParser {

    public static List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
            i++;
        }

        result.add(current.toString());
        return result;
    }

}
