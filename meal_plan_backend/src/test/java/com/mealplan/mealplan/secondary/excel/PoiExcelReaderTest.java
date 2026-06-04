package com.mealplan.mealplan.secondary.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PoiExcelReaderTest {

    private final PoiExcelReader reader = new PoiExcelReader();

    private byte[] buildXlsx(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("식단표");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void extracts_all_non_blank_tokens_from_xlsx() throws Exception {
        byte[] xlsx = buildXlsx(new String[][]{
                {"월", "화", "수"},
                {"흰쌀밥", "흑미밥", ""},
                {"미역국", "된장국", "육개장"}
        });

        List<String> tokens = reader.extractTokens("plan.xlsx", xlsx);

        assertThat(tokens).contains("월", "화", "수", "흰쌀밥", "흑미밥", "미역국", "된장국", "육개장");
        assertThat(tokens).doesNotContain("");
    }

    @Test
    void rejects_non_xlsx_extension() throws Exception {
        byte[] xlsx = buildXlsx(new String[][]{{"흰쌀밥"}});

        assertThatThrownBy(() -> reader.extractTokens("plan.csv", xlsx))
                .isInstanceOf(InvalidExcelFileException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    void rejects_corrupt_file_with_xlsx_extension() {
        byte[] notReallyXlsx = "this is plain text, not an excel".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> reader.extractTokens("fake.xlsx", notReallyXlsx))
                .isInstanceOf(InvalidExcelFileException.class);
    }
}
