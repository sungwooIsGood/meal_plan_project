package com.mealplan.mealplan.domain.exception;

/**
 * 업로드된 파일이 (형식상 유효한 엑셀이더라도) 급식 식단표 내용이 아닐 때 발생.
 *
 * <p>분류 가능한 메뉴 항목이 하나도 도출되지 않으면 식단표로 간주하지 않는다.
 * 웹 어댑터에서 422(Unprocessable Entity)로 매핑된다.
 */
public class NotMealPlanContentException extends RuntimeException {

    public NotMealPlanContentException(String message) {
        super(message);
    }
}
