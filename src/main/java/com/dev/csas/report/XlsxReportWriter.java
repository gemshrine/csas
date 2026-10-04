package com.dev.csas.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class XlsxReportWriter {

    public byte[] write(List<XlsxReportSheet> reportSheets) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (XlsxReportSheet reportSheet : reportSheets) {
                addSheet(workbook, reportSheet, titleStyle, headerStyle);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new XlsxReportGenerationException("Не удалось сформировать XLSX-отчёт", exception);
        }
    }

    private void addSheet(
            XSSFWorkbook workbook,
            XlsxReportSheet reportSheet,
            CellStyle titleStyle,
            CellStyle headerStyle
    ) {
        Sheet sheet = workbook.createSheet(reportSheet.name());
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportSheet.title());
        titleCell.setCellStyle(titleStyle);

        for (String context : reportSheet.context()) {
            sheet.createRow(rowIndex++).createCell(0).setCellValue(context);
        }
        rowIndex++;
        int headerIndex = rowIndex;
        Row headerRow = sheet.createRow(rowIndex++);
        for (int columnIndex = 0; columnIndex < reportSheet.columns().size(); columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(reportSheet.columns().get(columnIndex));
            cell.setCellStyle(headerStyle);
            int width = Math.max(12, Math.min(50, reportSheet.columns().get(columnIndex).length() + 3));
            sheet.setColumnWidth(columnIndex, width * 256);
        }

        for (List<Object> values : reportSheet.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                writeValue(row.createCell(columnIndex), values.get(columnIndex));
            }
        }
        if (!reportSheet.columns().isEmpty()) {
            sheet.createFreezePane(0, headerIndex + 1);
            if (!reportSheet.rows().isEmpty()) {
                sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        headerIndex, rowIndex - 1, 0, reportSheet.columns().size() - 1
                ));
            }
        }
    }

    private void writeValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.toString());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
