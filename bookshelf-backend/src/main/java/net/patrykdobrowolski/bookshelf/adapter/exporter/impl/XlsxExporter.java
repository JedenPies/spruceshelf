package net.patrykdobrowolski.bookshelf.adapter.exporter.impl;

import jakarta.inject.Named;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportData;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportResult;
import net.patrykdobrowolski.bookshelf.adapter.exporter.Exporter;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

@Named
public class XlsxExporter implements Exporter {

    @Override
    public boolean supports(ExportFormat format) {
        return ExportFormat.XLSX == format;
    }

    @Override
    public ExportResult export(ExportData exportData) throws ExportException.ExportFailedException {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(exportData.title());
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = exportData.headers().stream().map(ExportData.Header::name).toList().toArray(new String[0]);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (ExportData.Row dataRow : exportData.rows()) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String cellValue : dataRow.values()) {
                    row.createCell(colNum++).setCellValue(cellValue);
                }
            }

            // 4. Estetyka: Automatyczne dopasowanie szerokości kolumn do tekstu
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 5. Zapis do strumienia i zwrot jako tablica bajtów
            workbook.write(out);
            return ExportResult.of(out.toByteArray());

        } catch (Exception e) {
            throw new ExportException.ExportFailedException("XLSX export failed", e);
        }
    }
}
