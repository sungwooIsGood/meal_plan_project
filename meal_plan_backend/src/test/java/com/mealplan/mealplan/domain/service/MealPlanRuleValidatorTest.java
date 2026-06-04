package com.mealplan.mealplan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import com.mealplan.mealplan.domain.entity.StapleType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealPlanRuleValidatorTest {

    private final MealPlanRuleValidator validator = new MealPlanRuleValidator();

    private PlannedMeal meal(LocalDate date, MealType type, MenuItem... items) {
        return new PlannedMeal(date, type, List.of(items));
    }

    private MenuItem item(String name, MenuCategory c) {
        return MenuItem.of(name, c);
    }

    @Test
    void valid_when_rice_repeats_after_window() {
        // 밥류 7일 규칙: 1일과 8일은 간격 7 → OK
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)),
                meal(LocalDate.of(2026, 6, 8), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)));

        assertThat(validator.isValid(meals)).isTrue();
    }

    @Test
    void invalid_when_rice_repeats_within_7_days() {
        // 1일과 5일은 간격 4 < 7 → 위반
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)),
                meal(LocalDate.of(2026, 6, 5), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)));

        assertThat(validator.validate(meals)).hasSize(1);
    }

    @Test
    void kimchi_window_is_4_days() {
        // 김치 4일: 1일과 4일 간격 3 < 4 → 위반, 1일과 5일 간격 4 → OK
        List<PlannedMeal> bad = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("배추김치", MenuCategory.KIMCHI)),
                meal(LocalDate.of(2026, 6, 4), MealType.BREAKFAST, item("배추김치", MenuCategory.KIMCHI)));
        assertThat(validator.isValid(bad)).isFalse();

        List<PlannedMeal> ok = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("배추김치", MenuCategory.KIMCHI)),
                meal(LocalDate.of(2026, 6, 5), MealType.BREAKFAST, item("배추김치", MenuCategory.KIMCHI)));
        assertThat(validator.isValid(ok)).isTrue();
    }

    @Test
    void window_applies_across_meal_types_combined_timeline() {
        // 끼니 통합: 1일 조식 제육 ↔ 3일 석식 제육, 주찬 30일 window → 위반
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("제육볶음", MenuCategory.MAIN_DISH)),
                meal(LocalDate.of(2026, 6, 3), MealType.DINNER, item("제육볶음", MenuCategory.MAIN_DISH)));

        assertThat(validator.validate(meals)).anyMatch(v -> v.contains("제육볶음"));
    }

    @Test
    void same_day_cross_meal_duplicate_is_violation() {
        // 같은 날 조식·중식에 동일 메뉴 → 위반
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("미역국", MenuCategory.SOUP)),
                meal(LocalDate.of(2026, 6, 1), MealType.LUNCH, item("미역국", MenuCategory.SOUP)));

        assertThat(validator.validate(meals)).anyMatch(v -> v.contains("같은 날"));
    }

    @Test
    void empty_meals_is_valid() {
        assertThat(validator.isValid(List.of())).isTrue();
    }

    @Test
    void simple_rice_allowed_to_repeat_after_7_days() {
        // 흰쌀밥(일반 곡물밥, 7일): 1일과 8일은 간격 7 → OK
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)),
                meal(LocalDate.of(2026, 6, 8), MealType.BREAKFAST, item("흰쌀밥", MenuCategory.STAPLE_FOOD)));
        assertThat(validator.isValid(meals)).isTrue();
    }

    @Test
    void special_staple_uses_30_day_window() {
        // 일품 주식(DISH, 30일): 1일과 10일은 간격 9 < 30 → 위반
        List<PlannedMeal> bad = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST,
                        MenuItem.of("새우볶음밥", MenuCategory.STAPLE_FOOD, StapleType.DISH)),
                meal(LocalDate.of(2026, 6, 10), MealType.BREAKFAST,
                        MenuItem.of("새우볶음밥", MenuCategory.STAPLE_FOOD, StapleType.DISH)));
        assertThat(validator.isValid(bad)).isFalse();
    }

    @Test
    void kongnamul_bap_as_dish_uses_30_days() {
        // 콩나물밥을 일품(DISH)으로 분류하면 30일 규칙 → 8일 간격이면 위반
        List<PlannedMeal> meals = List.of(
                meal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST,
                        MenuItem.of("콩나물밥", MenuCategory.STAPLE_FOOD, StapleType.DISH)),
                meal(LocalDate.of(2026, 6, 9), MealType.BREAKFAST,
                        MenuItem.of("콩나물밥", MenuCategory.STAPLE_FOOD, StapleType.DISH)));
        assertThat(validator.isValid(meals)).isFalse();
    }
}
