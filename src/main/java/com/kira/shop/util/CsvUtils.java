package com.kira.shop.util;

import java.util.ArrayList;
import java.util.List;

/** Мінімальне форматування CSV без зовнішніх бібліотек. */
public final class CsvUtils {
    private static final char SEPARATOR = ';';

    private CsvUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    public static String join(String... cells) {
        List<String> escaped = new ArrayList<>();
        for (String cell : cells) {
            escaped.add(escape(cell));
        }
        return String.join(String.valueOf(SEPARATOR), escaped);
    }

    public static String[] split(String line) {
        return line.split(String.valueOf(SEPARATOR), -1);
    }
}
