package com.mealplan.mealplan.primary.rest.dto;

/**
 * 식단표 삭제 결과 응답.
 *
 * @param deletedCount 실제로 삭제된 개수
 */
public record DeleteMealPlansResponse(long deletedCount) {
}
