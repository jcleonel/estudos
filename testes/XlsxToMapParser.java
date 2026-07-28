package br.com.experian.customer.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public final class XlsxToMapParser {

    private XlsxToMapParser() {
    }

    /**
     * Lê a primeira aba do XLSX e devolve uma lista de maps usando a primeira linha como header.
     * Células são convertidas para String via DataFormatter.
     */
    public static List<Map<String, String>> parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();

            int firstRowNum = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(firstRowNum);
            if (headerRow == null) {
                return List.of();
            }

            List<String> headers = readHeaders(headerRow, formatter);

            return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(
                                    sheet.rowIterator(),
                                    Spliterator.ORDERED
                            ),
                            false
                    )
                    .filter(row -> row.getRowNum() > firstRowNum)
                    .map(row -> readRow(row, headers, formatter))
                    .filter(XlsxToMapParser::hasAnyValue)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private static List<String> readHeaders(
            Row headerRow,
            DataFormatter formatter) {

        return IntStream.range(0, headerRow.getLastCellNum())
                .mapToObj(column -> formatHeader(headerRow, column, formatter))
                .collect(
                        ArrayList::new,
                        XlsxToMapParser::addUniqueHeader,
                        List::addAll
                );
    }

    private static String formatHeader(
            Row headerRow,
            int column,
            DataFormatter formatter) {

        Cell cell = headerRow.getCell(column);
        return cell != null ? formatter.formatCellValue(cell).trim() : "";
    }

    private static void addUniqueHeader(
            List<String> headers,
            String rawHeader) {

        headers.add(uniqueHeader(rawHeader, headers));
    }

    private static String uniqueHeader(
            String rawHeader,
            List<String> headers) {

        if (rawHeader.isBlank()) {
            return rawHeader;
        }

        return IntStream.rangeClosed(1, headers.size() + 1)
                .mapToObj(occurrence -> occurrence == 1
                        ? rawHeader
                        : rawHeader + "__" + occurrence)
                .filter(candidate -> !headers.contains(candidate))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi possível gerar um cabeçalho XLSX único."
                ));
    }

    private static Map<String, String> readRow(
            Row row,
            List<String> headers,
            DataFormatter formatter) {

        return IntStream.range(0, headers.size())
                .collect(
                        LinkedHashMap::new,
                        (values, column) -> putCellValue(
                                values,
                                row,
                                headers.get(column),
                                column,
                                formatter
                        ),
                        LinkedHashMap::putAll
                );
    }

    private static void putCellValue(
            Map<String, String> values,
            Row row,
            String header,
            int column,
            DataFormatter formatter) {

        if (header == null || header.isBlank()) {
            return;
        }

        Cell cell = row.getCell(column);
        String value = cell != null ? formatter.formatCellValue(cell) : "";
        values.put(header, value != null ? value.trim() : "");
    }

    private static boolean hasAnyValue(Map<String, String> values) {
        return values.values().stream()
                .anyMatch(value -> value != null && !value.isBlank());
    }
}
