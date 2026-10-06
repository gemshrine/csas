package com.dev.csas.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PdfReportWriter {

    private static final float MARGIN = 28;
    private static final float CELL_PADDING = 3;
    private static final float MAX_CELL_FONT_SIZE = 8;
    private static final float MIN_CELL_FONT_SIZE = 5;
    private static final int MAX_CELL_LINES = 7;

    public byte[] write(byte[] xlsx) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx));
             PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream regularFontStream = new ClassPathResource(
                     "fonts/LiberationSans-Regular.ttf").getInputStream();
             InputStream boldFontStream = new ClassPathResource(
                     "fonts/LiberationSans-Bold.ttf").getInputStream()) {
            PDFont regularFont = PDType0Font.load(document, regularFontStream);
            PDFont boldFont = PDType0Font.load(document, boldFontStream);
            DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("ru-RU"));
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                renderSheet(document, sheet, formatter, regularFont, boldFont);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new PdfReportGenerationException("Не удалось сформировать PDF-отчёт", exception);
        }
    }

    private void renderSheet(
            PDDocument document,
            org.apache.poi.ss.usermodel.Sheet sheet,
            DataFormatter formatter,
            PDFont regularFont,
            PDFont boldFont
    ) throws IOException {
        List<List<String>> rows = readRows(sheet, formatter);
        if (rows.isEmpty()) {
            return;
        }

        String title = rows.getFirst().isEmpty() || rows.getFirst().getFirst().isBlank()
                ? sheet.getSheetName()
                : rows.getFirst().getFirst();
        int headerIndex = findHeaderIndex(rows);
        List<String> context = rows.subList(1, Math.max(1, headerIndex - 1)).stream()
                .map(row -> row.stream().filter(value -> !value.isBlank()).findFirst().orElse(""))
                .filter(value -> !value.isBlank())
                .toList();
        List<String> headers = rows.get(headerIndex);
        List<List<String>> body = rows.subList(Math.min(headerIndex + 1, rows.size()), rows.size());
        float pageWidth = headers.size() > 9 ? PDRectangle.A3.getHeight() : PDRectangle.A4.getHeight();
        float pageHeight = headers.size() > 9 ? PDRectangle.A3.getWidth() : PDRectangle.A4.getWidth();
        float availableWidth = pageWidth - 2 * MARGIN;
        float[] columnWidths = columnWidths(headers, body, availableWidth);
        float fontSize = Math.max(MIN_CELL_FONT_SIZE,
                Math.min(MAX_CELL_FONT_SIZE, availableWidth / Math.max(1, headers.size()) / 6));

        PageState page = newPage(document, pageWidth, pageHeight, title, context, headers, columnWidths,
                fontSize, regularFont, boldFont);
        for (List<String> row : body) {
            float rowHeight = rowHeight(row, columnWidths, fontSize, regularFont);
            if (page.y - rowHeight < MARGIN) {
                page.content.close();
                page = newPage(document, pageWidth, pageHeight, title, context, headers, columnWidths,
                        fontSize, regularFont, boldFont);
            }
            drawRow(page.content, row, page.y, rowHeight, columnWidths, fontSize, regularFont);
            page.y -= rowHeight;
        }
        page.content.close();
    }

    private List<List<String>> readRows(
            org.apache.poi.ss.usermodel.Sheet sheet,
            DataFormatter formatter
    ) {
        List<List<String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                rows.add(List.of());
                continue;
            }
            int lastCell = Math.max(0, row.getLastCellNum());
            List<String> values = new ArrayList<>(lastCell);
            for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
                values.add(row.getCell(cellIndex) == null ? "" : formatter.formatCellValue(row.getCell(cellIndex)));
            }
            rows.add(values);
        }
        while (!rows.isEmpty() && rows.getLast().stream().allMatch(String::isBlank)) {
            rows.removeLast();
        }
        return rows;
    }

    private int findHeaderIndex(List<List<String>> rows) {
        for (int index = 1; index < rows.size() - 1; index++) {
            if (rows.get(index).stream().allMatch(String::isBlank)) {
                int next = index + 1;
                while (next < rows.size() && rows.get(next).stream().allMatch(String::isBlank)) {
                    next++;
                }
                return Math.min(next, rows.size() - 1);
            }
        }
        return Math.min(1, rows.size() - 1);
    }

    private float[] columnWidths(List<String> headers, List<List<String>> rows, float availableWidth) {
        float[] weights = new float[headers.size()];
        float total = 0;
        for (int column = 0; column < headers.size(); column++) {
            int maxLength = headers.get(column).length();
            for (List<String> row : rows) {
                if (column < row.size()) {
                    maxLength = Math.max(maxLength, row.get(column).length());
                }
            }
            weights[column] = Math.max(5, Math.min(28, maxLength));
            total += weights[column];
        }
        float[] widths = new float[headers.size()];
        for (int column = 0; column < widths.length; column++) {
            widths[column] = total == 0 ? availableWidth : availableWidth * weights[column] / total;
        }
        return widths;
    }

    private PageState newPage(
            PDDocument document,
            float pageWidth,
            float pageHeight,
            String title,
            List<String> context,
            List<String> headers,
            float[] columnWidths,
            float fontSize,
            PDFont regularFont,
            PDFont boldFont
    ) throws IOException {
        PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        float y = pageHeight - MARGIN;
        y = drawText(content, title, MARGIN, y, boldFont, 12, pageWidth - 2 * MARGIN) - 7;
        for (String line : context) {
            y = drawText(content, line, MARGIN, y, regularFont, 8, pageWidth - 2 * MARGIN) - 2;
        }
        y -= 5;
        float headerHeight = rowHeight(headers, columnWidths, fontSize, regularFont);
        drawRow(content, headers, y, headerHeight, columnWidths, fontSize, boldFont);
        return new PageState(content, y - headerHeight);
    }

    private float drawText(PDPageContentStream content, String value, float x, float y,
                           PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = wrap(value, maxWidth, font, fontSize, 2);
        content.beginText();
        content.setFont(font, fontSize);
        content.setNonStrokingColor(Color.BLACK);
        content.newLineAtOffset(x, y - fontSize);
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                content.newLineAtOffset(0, -fontSize - 1);
            }
            content.showText(lines.get(index));
        }
        content.endText();
        return y - lines.size() * (fontSize + 1);
    }

    private float rowHeight(List<String> values, float[] widths, float fontSize, PDFont font) throws IOException {
        int maxLines = 1;
        for (int column = 0; column < widths.length; column++) {
            String value = column < values.size() ? values.get(column) : "";
            maxLines = Math.max(maxLines, wrap(value, widths[column] - 2 * CELL_PADDING,
                    font, fontSize, MAX_CELL_LINES).size());
        }
        return maxLines * (fontSize + 1.5f) + 2 * CELL_PADDING;
    }

    private void drawRow(PDPageContentStream content, List<String> values, float top, float height,
                         float[] widths, float fontSize, PDFont font) throws IOException {
        float x = MARGIN;
        for (int column = 0; column < widths.length; column++) {
            float width = widths[column];
            content.setStrokingColor(Color.BLACK);
            content.setLineWidth(0.35f);
            content.addRect(x, top - height, width, height);
            content.stroke();
            String value = column < values.size() ? values.get(column) : "";
            List<String> lines = wrap(value, width - 2 * CELL_PADDING, font, fontSize, MAX_CELL_LINES);
            content.beginText();
            content.setFont(font, fontSize);
            content.setNonStrokingColor(Color.BLACK);
            content.newLineAtOffset(x + CELL_PADDING, top - CELL_PADDING - fontSize);
            for (int line = 0; line < lines.size(); line++) {
                if (line > 0) {
                    content.newLineAtOffset(0, -fontSize - 1.5f);
                }
                content.showText(lines.get(line));
            }
            content.endText();
            x += width;
        }
    }

    private List<String> wrap(String value, float maxWidth, PDFont font, float fontSize, int maxLines)
            throws IOException {
        if (value.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getStringWidth(candidate) * fontSize / 1000 <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
                if (lines.size() == maxLines) {
                    return lines;
                }
            }
            if (font.getStringWidth(word) * fontSize / 1000 <= maxWidth) {
                current.append(word);
            } else {
                StringBuilder fragment = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int codePoint = word.codePointAt(offset);
                    String character = new String(Character.toChars(codePoint));
                    String next = fragment + character;
                    if (!fragment.isEmpty() && font.getStringWidth(next) * fontSize / 1000 > maxWidth) {
                        lines.add(fragment.toString());
                        fragment.setLength(0);
                        if (lines.size() == maxLines) {
                            return lines;
                        }
                    }
                    fragment.append(character);
                    offset += Character.charCount(codePoint);
                }
                current.append(fragment);
            }
        }
        if (!current.isEmpty() && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private static final class PageState {
        private final PDPageContentStream content;
        private float y;

        private PageState(PDPageContentStream content, float y) {
            this.content = content;
            this.y = y;
        }
    }
}
