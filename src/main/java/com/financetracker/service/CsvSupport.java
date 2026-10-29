package com.financetracker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CsvSupport {
    private CsvSupport() {
    }

    /**
     * RFC 4180 style fields, including quoted commas, escaped quotes, and multiline text.
     */
    public static List<List<String>> parse(String text) {
        if (text.length() > 2_000_000) throw new IllegalArgumentException("CSV exceeds 2 MB.");
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else field.append(c);
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(field.toString());
                field.setLength(0);
                closed = false;
                if (c != ',') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    if (row.stream().anyMatch(v -> !v.isBlank())) rows.add(List.copyOf(row));
                    row.clear();
                }
            } else if (c == '"' && field.isEmpty() && !closed) quoted = true;
            else {
                if (closed || c == '"') throw new IllegalArgumentException("Malformed CSV quoting.");
                field.append(c);
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote.");
        if (!field.isEmpty() || !row.isEmpty() || closed) {
            row.add(field.toString());
            rows.add(List.copyOf(row));
        }
        if (rows.size() > 5001) throw new IllegalArgumentException("Import at most 5,000 transactions at once.");
        return rows;
    }

    public static String cell(Object value) {
        String s = value == null ? "" : value.toString();
        String trimmed = s.stripLeading();
        if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) || s.startsWith("\t") || s.startsWith("\r") || s.startsWith("\n"))
            s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static String row(Object... values) {
        return Arrays.stream(values).map(CsvSupport::cell).collect(java.util.stream.Collectors.joining(",")) + "\r\n";
    }
}
