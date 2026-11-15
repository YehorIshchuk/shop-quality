package com.kira.shop.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Табличне ядро звіту (колишній BaseReport). Тепер це final-компонент без нащадків:
 * звіти отримують його через конструктор і делегують йому зберігання та рендеринг рядків.
 */
public final class ReportTable {
    private final String separator;
    private final List<List<String>> rows = new ArrayList<>();

    public ReportTable(String separator) {
        this.separator = separator;
    }

    public void addRow(List<String> cells) {
        rows.add(List.copyOf(cells));
    }

    public List<List<String>> getRows() {
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

    public String formatRow(List<String> cells) {
        return String.join(separator, cells);
    }

    /** Рендер із заголовком, підсумком і декоратором рядка — точки розширення без перевизначення. */
    public String render(String header, String footer, UnaryOperator<String> rowDecorator) {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (List<String> row : rows) {
            sb.append(rowDecorator.apply(formatRow(row))).append('\n');
        }
        return sb.append(footer).toString();
    }

    public String renderCsv() {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            sb.append(String.join(";", row)).append('\n');
        }
        return sb.toString();
    }
}
