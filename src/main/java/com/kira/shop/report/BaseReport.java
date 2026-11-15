package com.kira.shop.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Базовий табличний звіт: заголовок, рядки, підсумок, текстовий та CSV-вивід. */
public class BaseReport {
    private String title;
    private String separator = " | ";
    private final List<String[]> rows = new ArrayList<>();

    public BaseReport(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void addRow(String... cells) {
        rows.add(cells.clone());
    }

    public List<String[]> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public int rowCount() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void clear() {
        rows.clear();
    }

    public String header() {
        return "=== " + title + " ===";
    }

    public String formatRow(List<String> cells) {
        return String.join(separator, cells);
    }

    public String footer() {
        return "Rows: " + rowCount();
    }

    public String render() {
        StringBuilder sb = new StringBuilder(header()).append('\n');
        for (String[] row : rows) {
            sb.append(formatRow(List.of(row))).append('\n');
        }
        return sb.append(footer()).toString();
    }

    public String renderCsv() {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append(String.join(";", row)).append('\n');
        }
        return sb.toString();
    }
}
