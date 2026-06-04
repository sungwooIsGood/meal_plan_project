package com.mealplan.mealplan.primary.rest.dto;

import com.mealplan.mealplan.application.usecase.GenerateMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 식단 생성 결과 응답 DTO.
 *
 * @param totalMeals     생성된 끼니 수
 * @param usedAi         AI 생성 사용 여부 (false면 결정론적 생성기 사용)
 * @param ruleViolations 규칙 위반 메시지(비어 있으면 완전 준수)
 * @param days           날짜별 끼니 구성
 */
public record GenerateMealPlanResponse(
        int totalMeals,
        boolean usedAi,
        List<String> ruleViolations,
        List<DayPlan> days) {

    /** 하루 단위 묶음. */
    public record DayPlan(String date, List<Meal> meals) {
    }

    /** 한 끼. */
    public record Meal(String mealType, Map<String, List<String>> itemsByCategory) {
    }

    public static GenerateMealPlanResponse from(GenerateMealPlanUseCase.Result result) {
        // 날짜 -> (끼니순) 끼니 목록
        Map<LocalDate, List<PlannedMeal>> byDate = new LinkedHashMap<>();
        result.meals().stream()
                .sorted((a, b) -> {
                    int c = a.date().compareTo(b.date());
                    return c != 0 ? c : a.mealType().compareTo(b.mealType());
                })
                .forEach(m -> byDate.computeIfAbsent(m.date(), k -> new java.util.ArrayList<>()).add(m));

        List<DayPlan> days = new java.util.ArrayList<>();
        byDate.forEach((date, meals) -> {
            List<Meal> mealDtos = meals.stream().map(GenerateMealPlanResponse::toMeal).toList();
            days.add(new DayPlan(date.toString(), mealDtos));
        });

        return new GenerateMealPlanResponse(
                result.meals().size(), result.usedAi(), result.ruleViolations(), days);
    }

    private static Meal toMeal(PlannedMeal meal) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (MenuCategory category : MenuCategory.values()) {
            List<String> names = meal.namesOf(category);
            if (!names.isEmpty()) {
                grouped.put(category.name(), names);
            }
        }
        return new Meal(meal.mealType().name(), grouped);
    }
}
