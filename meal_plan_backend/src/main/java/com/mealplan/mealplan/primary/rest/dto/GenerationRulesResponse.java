package com.mealplan.mealplan.primary.rest.dto;

import com.mealplan.mealplan.domain.entity.GenerationRules;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기본 생성 규칙 안내 응답 DTO (프론트엔드 표시용).
 *
 * @param notice           안내 문구
 * @param dailyComposition 하루 구성
 * @param noRepeatWithinDays 카테고리별 중복 금지 일수 (끼니 통합 기준)
 * @param crossMealRules   끼니 간 규칙
 */
public record GenerationRulesResponse(
        String notice,
        List<String> dailyComposition,
        Map<String, Integer> noRepeatWithinDays,
        List<String> crossMealRules) {

    public static GenerationRulesResponse current() {
        Map<String, Integer> days = new LinkedHashMap<>();
        GenerationRules.NO_REPEAT_WITHIN_DAYS.forEach((cat, d) ->
                days.put(cat.name() + "(" + cat.koreanName() + ")", d));
        return new GenerationRulesResponse(
                "요구사항을 입력하지 않으면 아래 기본 규칙대로 식단을 생성합니다.",
                GenerationRules.DAILY_COMPOSITION,
                days,
                GenerationRules.CROSS_MEAL_RULES);
    }
}
