package com.mealplan.mealplan.application.output;

import java.util.List;

/**
 * 업로드된 식단표 파일에서 텍스트 토큰(메뉴 후보 문자열)을 추출하는 Output Port.
 *
 * <p>구현체: {@code secondary/excel/PoiExcelReader}.
 */
public interface MealPlanFileReaderPort {

    /**
     * 파일에서 모든 텍스트 토큰을 추출한다.
     *
     * @param originalFileName 원본 파일명 (확장자 검증용)
     * @param content          파일 바이트
     * @return 추출된 텍스트 토큰 목록 (중복/공백 정리 전 원시 토큰)
     * @throws com.mealplan.mealplan.domain.exception.InvalidExcelFileException
     *         .xlsx가 아니거나 파싱할 수 없을 때
     */
    List<String> extractTokens(String originalFileName, byte[] content);
}
