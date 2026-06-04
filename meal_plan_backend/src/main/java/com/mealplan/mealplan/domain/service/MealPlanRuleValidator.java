package com.mealplan.mealplan.domain.service;

import com.mealplan.mealplan.domain.entity.GenerationRules;
import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 생성된 식단이 중복 규칙을 지키는지 결정론적으로 검증하는 도메인 서비스.
 *
 * <p>LLM 생성 결과를 신뢰하지 않고 이 검증기로 실제 규칙 준수를 보장한다.
 * 순수 로직(외부 의존 0)이라 단위 테스트로 모든 규칙을 검증한다.
 *
 * <p>검증 규칙:
 * <ol>
 *   <li><b>카테고리별 중복 금지 기간</b>: 조식·중식·석식을 <b>합친 전체 타임라인</b> 기준으로,
 *       카테고리별 N일 이내 동일 메뉴가 다시 나올 수 없다
 *       ({@link GenerationRules#NO_REPEAT_WITHIN_DAYS}). 끼니가 달라도 동일하게 적용된다
 *       (예: 1일 조식 제육 → 30일 이내 어떤 끼니에도 제육 금지, 주찬 window=30).</li>
 *   <li><b>같은 날 끼니 간 중복 금지</b>: 같은 날짜 안에서는 카테고리와 무관하게 동일 메뉴가
 *       두 끼니에 동시에 나올 수 없다.</li>
 * </ol>
 */
public class MealPlanRuleValidator {

    /**
     * @return 위반 메시지 목록. 비어 있으면 규칙을 모두 만족.
     */
    public List<String> validate(List<PlannedMeal> meals) {
        List<String> violations = new ArrayList<>();
        violations.addAll(checkCategoryRepeatWindow(meals));
        violations.addAll(checkSameDayCrossMealDuplicates(meals));
        return violations;
    }

    public boolean isValid(List<PlannedMeal> meals) {
        return validate(meals).isEmpty();
    }

    /**
     * 규칙 1: 전체 타임라인(끼니 통합) 기준 카테고리별 N일 이내 동일 메뉴 금지.
     *
     * <p>끼니로 구분하지 않고 모든 메뉴 등장을 날짜순으로 모아 검사한다.
     */
    private List<String> checkCategoryRepeatWindow(List<PlannedMeal> meals) {
        List<String> violations = new ArrayList<>();
        // (category|menuName) -> 등장한 날짜들 (전체 끼니 통합)
        Map<String, List<LocalDate>> seen = new HashMap<>();

        List<PlannedMeal> sorted = meals.stream()
                .sorted((a, b) -> {
                    int byDate = a.date().compareTo(b.date());
                    return byDate != 0 ? byDate : a.mealType().compareTo(b.mealType());
                })
                .toList();

        for (PlannedMeal meal : sorted) {
            for (MenuItem item : meal.items()) {
                int window = GenerationRules.repeatWindowDays(item);
                if (window <= 0) {
                    continue;
                }
                String key = item.category().name() + "|" + item.name();
                List<LocalDate> dates = seen.computeIfAbsent(key, k -> new ArrayList<>());
                for (LocalDate prev : dates) {
                    long gap = Math.abs(meal.date().toEpochDay() - prev.toEpochDay());
                    if (gap < window) {
                        violations.add(String.format(
                                "[%s] '%s'(%s)이 %d일 간격으로 재등장 (%s → %s) — 규칙: 끼니 통합 %d일 내 금지",
                                item.category().koreanName(), item.name(), item.category().name(),
                                gap, prev, meal.date(), window));
                    }
                }
                dates.add(meal.date());
            }
        }
        return violations;
    }

    /** 규칙 2: 같은 날짜 안에서 끼니 간 동일 메뉴 금지 (카테고리 무관). */
    private List<String> checkSameDayCrossMealDuplicates(List<PlannedMeal> meals) {
        List<String> violations = new ArrayList<>();
        Map<LocalDate, Map<String, MealType>> perDay = new HashMap<>();

        List<PlannedMeal> sorted = meals.stream()
                .sorted((a, b) -> {
                    int byDate = a.date().compareTo(b.date());
                    return byDate != 0 ? byDate : a.mealType().compareTo(b.mealType());
                })
                .toList();

        for (PlannedMeal meal : sorted) {
            Map<String, MealType> namesToday = perDay.computeIfAbsent(meal.date(), k -> new HashMap<>());
            for (MenuItem item : meal.items()) {
                MealType prevMealType = namesToday.get(item.name());
                if (prevMealType != null && prevMealType != meal.mealType()) {
                    violations.add(String.format(
                            "[%s] '%s'이 같은 날 %s·%s 끼니에 중복 — 같은 날 끼니 간 중복 금지",
                            meal.date(), item.name(),
                            prevMealType.koreanName(), meal.mealType().koreanName()));
                }
                namesToday.putIfAbsent(item.name(), meal.mealType());
            }
        }
        return violations;
    }
}
