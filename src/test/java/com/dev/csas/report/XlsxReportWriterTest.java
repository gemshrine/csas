package com.dev.csas.report;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XlsxReportWriterTest {

    private final XlsxReportWriter writer = new XlsxReportWriter();

    @Test
    void writesHeadersAndKeepsEmptyResultsBlank() throws Exception {
        XlsxReportSheet sheet = new XlsxReportSheet(
                "Отчёт",
                "Проверка XLSX",
                List.of("Группа: ИВТ-22"),
                List.of("Студент", "Рейтинг, %", "Итог"),
                List.of(Arrays.asList("Иванов Иван", null, 87.5))
        );

        byte[] bytes = writer.write(List.of(sheet));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var exported = workbook.getSheet("Отчёт");
            assertNotNull(exported);
            assertEquals("Проверка XLSX", exported.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Студент", exported.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Иванов Иван", exported.getRow(4).getCell(0).getStringCellValue());
            assertEquals("Рейтинг, %", exported.getRow(3).getCell(1).getStringCellValue());
            assertEquals(CellType.BLANK, exported.getRow(4).getCell(1).getCellType());
            assertEquals(87.5, exported.getRow(4).getCell(2).getNumericCellValue());
        }
    }
}
