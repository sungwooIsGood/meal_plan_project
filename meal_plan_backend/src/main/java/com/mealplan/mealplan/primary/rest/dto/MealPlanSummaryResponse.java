package com.mealplan.mealplan.primary.rest.dto;

import com.mealplan.mealplan.domain.entity.MealPlan;
import java.time.Instant;

/**
 * 식단표 목록용 요약 응답 DTO. 메뉴 상세 없이 식별 정보만 담는다.
 *
 * @param id             식단표 ID
 * @param sourceFileName 원본 파일명
 * @param importedAt     import 시각
 * @param totalItems     분류된 메뉴 총 개수
 */
public record MealPlanSummaryResponse(
        String id,
        String sourceFileName,
        Instant importedAt,
        int totalItems) {

    public static MealPlanSummaryResponse from(MealPlan mealPlan) {
        return new MealPlanSummaryResponse(
                mealPlan.id().value(),
                mealPlan.sourceFileName(),
                mealPlan.importedAt(),
                mealPlan.itemCount());
    }
}
