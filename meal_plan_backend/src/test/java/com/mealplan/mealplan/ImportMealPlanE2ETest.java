package com.mealplan.mealplan;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealplan.mealplan.support.MongoTestContainerConfig;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 전체 스택 E2E 검증: HTTP 멀티파트 업로드 → POI 추출 → 규칙기반 분류 → MongoDB 저장 → 응답.
 * Testcontainers(mongo:7.0) + 실제 내장 톰캣(random port) 사용. 도커 데몬이 실행 중이어야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MongoTestContainerConfig.class)
class ImportMealPlanE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private byte[] realMealPlanXlsx() throws Exception {
        String[][] rows = {
                {"2026년 6월", "학생 식단표", "신성고등학교"},
                {"월", "화", "수", "목", "금"},
                {"흰쌀밥", "흑미밥", "수수밥", "콩나물밥&양념장", "김치볶음밥&계란후라이"},
                {"우삼겹된장찌개", "계란파국", "맑은버섯국", "어니언스프", "얼큰어묵국"},
                {"돈육김치찜", "짜장찜닭", "우삼겹야채볶음", "돈육숙주불고기", "버팔로윙&해쉬브라운"},
                {"새우가득새우링/나쵸샐러드", "김말이튀김&야채튀김", "오믈렛&케찹/참나물유자무침", "냉파스타샐러드", "치즈스틱/애호박전"},
                {"콘푸로스트&우유/배추김치", "알타리김치", "파인애플/깍두기", "숭늉/배추김치/무피클", "석박지"},
        };
        return buildXlsx(rows);
    }

    private byte[] nonMealXlsx() throws Exception {
        String[][] rows = {
                {"분기별 매출 보고서", "2026 Q2"},
                {"고객사", "계약 금액", "담당자"},
                {"A상사", "1200000", "홍길동"},
                {"B물산", "3400000", "임꺽정"},
                {"영업이익 합계", "4600000", ""},
        };
        return buildXlsx(rows);
    }

    private byte[] buildXlsx(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sheet");
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

    private ResponseEntity<String> upload(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        return restTemplate.postForEntity(
                "/api/meal-plans/import", new HttpEntity<>(body, headers), String.class);
    }

    @Test
    void valid_meal_plan_returns_201_and_classifies_into_categories() throws Exception {
        ResponseEntity<String> response = upload(realMealPlanXlsx(), "2026-06.xlsx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response.getBody());
        assertThat(body.path("id").asText()).isNotBlank();
        assertThat(body.path("totalItems").asInt()).isGreaterThan(0);

        JsonNode byCategory = body.path("itemsByCategory");
        // 주식/국탕/김치/음료 등 핵심 카테고리가 채워졌는지 확인
        assertThat(byCategory.has("STAPLE_FOOD")).isTrue();
        assertThat(byCategory.has("SOUP")).isTrue();
        assertThat(byCategory.has("KIMCHI")).isTrue();
        assertThat(byCategory.path("BEVERAGE").toString()).contains("우유");
    }

    @Test
    void non_xlsx_file_returns_400() {
        byte[] notExcel = "this is plain text, not excel".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<String> response = upload(notExcel, "note.txt");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_EXCEL_FILE");
    }

    @Test
    void corrupt_xlsx_returns_400() {
        byte[] fake = "fake xlsx content".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<String> response = upload(fake, "fake.xlsx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_EXCEL_FILE");
    }

    @Test
    void valid_xlsx_without_meal_content_returns_422() throws Exception {
        ResponseEntity<String> response = upload(nonMealXlsx(), "sales-report.xlsx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("NOT_MEAL_PLAN_CONTENT");
    }
}
