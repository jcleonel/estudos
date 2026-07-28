package br.com.experian.customer.util;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxToMapParserTest {

    @Test
    void shouldKeepAcceptingColumnsBeyondKnownLayouts() throws IOException {
        byte[] xlsx = createWorkbook(
                new String[]{"a", "b", "c", "d", "e", "f", "g"},
                new String[]{"1", "2", "3", "4", "5", "6", "7"}
        );

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals(1, result.size());
        assertIterableEquals(
                List.of("a", "b", "c", "d", "e", "f", "g"),
                result.get(0).keySet()
        );
        assertEquals("7", result.get(0).get("g"));
    }

    @Test
    void shouldKeepIgnoringEmptyRowsEvenWhenLastPhysicalRowIsDistant()
            throws IOException {

        byte[] xlsx;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("dados");
            sheet.createRow(0).createCell(0).setCellValue("documento");
            sheet.createRow(1).createCell(0).setCellValue("123");

            Row distantRow = sheet.createRow(Constants.MAX_ROWS + 1);
            CellStyle style = workbook.createCellStyle();
            distantRow.createCell(0).setCellStyle(style);

            workbook.write(output);
            xlsx = output.toByteArray();
        }

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals(1, result.size());
        assertEquals("123", result.get(0).get("documento"));
    }

    @Test
    void shouldKeepReadingSparseRowsByDocumentCount() throws IOException {
        byte[] xlsx;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("dados");
            sheet.createRow(0).createCell(0).setCellValue("documento");
            sheet.createRow(1).createCell(0).setCellValue("123");
            sheet.createRow(Constants.MAX_ROWS + 1)
                    .createCell(0)
                    .setCellValue("456");

            workbook.write(output);
            xlsx = output.toByteArray();
        }

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals(2, result.size());
        assertEquals("123", result.get(0).get("documento"));
        assertEquals("456", result.get(1).get("documento"));
    }

    @Test
    void shouldPreserveLegacyDuplicateHeaderResolution() throws IOException {
        byte[] xlsx = createWorkbook(
                new String[]{"campo", "campo", "campo__2", "campo"},
                new String[]{"primeiro", "segundo", "terceiro", "quarto"}
        );

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals(1, result.size());
        assertIterableEquals(
                List.of("campo", "campo__2", "campo__2__2", "campo__3"),
                result.get(0).keySet()
        );
        assertEquals("primeiro", result.get(0).get("campo"));
        assertEquals("segundo", result.get(0).get("campo__2"));
        assertEquals("terceiro", result.get(0).get("campo__2__2"));
        assertEquals("quarto", result.get(0).get("campo__3"));
    }

    @Test
    void shouldKeepSkippingBlankHeadersAndBlankRows() throws IOException {
        byte[] xlsx = createWorkbook(
                new String[]{"documento", "", "origem"},
                new String[]{"", "ignorado", ""},
                new String[]{"123", "tambem ignorado", "PORTAL"}
        );

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals(1, result.size());
        assertIterableEquals(
                List.of("documento", "origem"),
                result.get(0).keySet()
        );
        assertEquals("123", result.get(0).get("documento"));
        assertEquals("PORTAL", result.get(0).get("origem"));
    }

    @Test
    void shouldKeepTrimmingCellValues() throws IOException {
        byte[] xlsx = createWorkbook(
                new String[]{"documento"},
                new String[]{"  123  "}
        );

        List<Map<String, String>> result = XlsxToMapParser.parse(xlsx);

        assertEquals("123", result.get(0).get("documento"));
    }

    @Test
    void shouldReturnEmptyListForEmptyFile() throws IOException {
        assertTrue(XlsxToMapParser.parse(new byte[0]).isEmpty());
    }

    private byte[] createWorkbook(
            String[] headers,
            String[]... rows) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("dados");
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                headerRow.createCell(column).setCellValue(headers[column]);
            }

            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] values = rows[rowIndex];
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
