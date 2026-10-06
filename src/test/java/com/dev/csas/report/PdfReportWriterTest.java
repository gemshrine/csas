package com.dev.csas.report;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportWriterTest {

    @Test
    void writesCyrillicReportAndPreservesTableValues() throws IOException {
        byte[] xlsx = new XlsxReportWriter().write(List.of(new XlsxReportSheet(
                "Итоговая ведомость",
                "Ведомость курса",
                List.of("Группа: ИВТ-22", "Дата формирования: 2026-10-06"),
                List.of("Студент", "Дисциплина", "Балл"),
                List.of(List.of("Иванов Иван", "Математическое моделирование", 87))
        )));

        byte[] pdf = new PdfReportWriter().write(xlsx);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(text).contains("Ведомость курса", "Группа: ИВТ-22", "Иванов Иван",
                    "Математическое моделирование", "87");
        }
    }

    @Test
    void splitsLongTableAcrossPagesAndRepeatsTheHeader() throws IOException {
        List<List<Object>> rows = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> List.<Object>of("Студент " + index, "Группа", index))
                .toList();
        byte[] xlsx = new XlsxReportWriter().write(List.of(new XlsxReportSheet(
                "Рейтинг",
                "Рейтинг студентов",
                List.of("Группа: ИВТ-22"),
                List.of("Студент", "Группа", "Рейтинг"),
                rows
        )));

        byte[] pdf = new PdfReportWriter().write(xlsx);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text).contains("Студент", "Студент 99", "Рейтинг студентов");
        }
    }
}
