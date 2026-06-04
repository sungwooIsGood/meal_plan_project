package com.mealplan.mealplan.primary.rest.dto;

import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 식단표 import 결과 응답 DTO. 도메인 객체를 직접 노출하지 않는다.
 *
 * @param id             저장된 식단표 ID
 * @param sourceFileName 원본 파일명
 * @param importedAt     import 시각
 * @param totalItems     분류된 메뉴 총 개수
 * @param itemsByCategory 카테고리별 메뉴명 목록
 */
public record ImportMealPlanResponse(
        String id,
        String sourceFileName,
        Instant importedAt,
        int totalItems,
        Map<String, List<String>> itemsByCategory) {

    public static ImportMealPlanResponse from(MealPlan mealPlan) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        // 카테고리 enum 선언 순서로 정렬하여 일관된 응답 제공
        for (MenuCategory category : MenuCategory.values()) {
            List<String> names = mealPlan.itemsByCategory().get(category);
            if (names != null && !names.isEmpty()) {
                grouped.put(category.name(), names);
            }
        }
        return new ImportMealPlanResponse(
                mealPlan.id().value(),
                mealPlan.sourceFileName(),
                mealPlan.importedAt(),
                mealPlan.itemCount(),
                grouped);
    }
}
