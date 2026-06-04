package com.mealplan.mealplan.secondary.excel;

import com.mealplan.mealplan.application.output.MealPlanFileReaderPort;
import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Apache POI 기반 .xlsx 텍스트 추출 어댑터 (Outbound).
 *
 * <p>{@link MealPlanFileReaderPort} 구현. .xlsx 확장자가 아니거나 OOXML로 파싱되지 않으면
 * {@link InvalidExcelFileException} 을 던진다.
 * (구형 .xls(HSSF)는 지원하지 않는다 — 사용자 요구: 엑셀(xlsx)만 허용.)
 */
@Component
public class PoiExcelReader implements MealPlanFileReaderPort {

    private static final String XLSX_EXTENSION = ".xlsx";

    @Override
    public List<String> extractTokens(String originalFileName, byte[] content) {
        validateExtension(originalFileName);

        List<String> tokens = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.KOREA);

        try (InputStream in = new ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String text = formatter.formatCellValue(cell);
                        if (text != null && !text.isBlank()) {
                            tokens.add(text.trim());
                        }
                    }
                }
            }
        } catch (NotOfficeXmlFileException e) {
            throw new InvalidExcelFileException(
                    "유효한 .xlsx 엑셀 파일이 아닙니다 (OOXML 형식 아님).", e);
        } catch (Exception e) {
            throw new InvalidExcelFileException(
                    "엑셀 파일을 읽을 수 없습니다. 파일이 손상되었거나 지원하지 않는 형식입니다.", e);
        }

        return tokens;
    }

    private void validateExtension(String originalFileName) {
        String lower = originalFileName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(XLSX_EXTENSION)) {
            throw new InvalidExcelFileException(
                    "엑셀(.xlsx) 파일만 업로드할 수 있습니다. 입력 파일명: " + originalFileName);
        }
    }
}
