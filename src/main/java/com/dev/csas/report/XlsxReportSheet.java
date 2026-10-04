package com.dev.csas.report;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public record XlsxReportSheet(
        String name,
        String title,
        List<String> context,
        List<String> columns,
        List<List<Object>> rows
) {
    public XlsxReportSheet {
        context = List.copyOf(context);
        columns = List.copyOf(columns);
        rows = rows.stream().map(row -> Collections.unmodifiableList(new ArrayList<>(row))).toList();
    }
}
