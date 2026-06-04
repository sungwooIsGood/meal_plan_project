package com.mealplan.mealplan.primary.rest.dto;

import java.util.List;

/**
 * 식단표 일괄 삭제 요청 body.
 *
 * @param ids 삭제할 식단표 ID 목록
 */
public record DeleteMealPlansRequest(List<String> ids) {
}
