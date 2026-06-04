package com.mealplan.mealplan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeterministicMealPlanGeneratorTest {

    private final DeterministicMealPlanGenerator generator = new DeterministicMealPlanGenerator();
    private final MealPlanRuleValidator validator = new MealPlanRuleValidator();

    /** 각 카테고리에 충분히 다양한 메뉴 풀 생성 → 규칙 충족 가능. 주찬 30일 규칙 대비 40종. */
    private Map<MenuCategory, List<MenuItem>> richPool() {
        return poolOf(40);
    }

    private Map<MenuCategory, List<MenuItem>> poolOf(int perCategory) {
        Map<MenuCategory, List<MenuItem>> pool = new EnumMap<>(MenuCategory.class);
        for (MenuCategory c : MenuCategory.values()) {
            List<MenuItem> items = new ArrayList<>();
            for (int i = 1; i <= perCategory; i++) {
                items.add(MenuItem.of(c.name() + "_" + i, c));
            }
            pool.put(c, items);
        }
        return pool;
    }

    private List<LocalDate> juneDates(int from, int to) {
        List<LocalDate> dates = new ArrayList<>();
        YearMonth ym = YearMonth.of(2026, 6);
        for (int d = from; d <= to; d++) {
            dates.add(ym.atDay(d));
        }
        return dates;
    }

    @Test
    void generated_plan_satisfies_all_rules_for_full_month_breakfast() {
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 30), List.of(MealType.BREAKFAST), richPool(), 1);

        assertThat(meals).hasSize(30);
        assertThat(validator.validate(meals)).isEmpty();
    }

    @Test
    void generated_plan_satisfies_rules_for_all_three_meal_types() {
        // 30일 × 3끼 = 90 끼니. 주찬 30일 윈도우(끼니 통합)를 지키려면 주찬이 90종 이상 필요.
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 30),
                List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
                poolOf(95), 1);

        assertThat(meals).hasSize(90); // 30일 * 3끼
        assertThat(validator.validate(meals)).isEmpty();
    }

    @Test
    void undersized_pool_surfaces_violations_but_still_generates() {
        // 주찬이 10종뿐인데 30일치 → 30일 윈도우를 못 지킴. 생성은 되지만 위반이 보고됨.
        Map<MenuCategory, List<MenuItem>> smallPool = poolOf(10);
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 30), List.of(MealType.BREAKFAST), smallPool, 1);

        assertThat(meals).hasSize(30); // 생성 자체는 보장
        assertThat(validator.validate(meals)).isNotEmpty(); // 풀 부족 → 위반 존재
    }

    @Test
    void each_meal_has_base_composition() {
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 3), List.of(MealType.LUNCH), richPool(), 1);

        for (PlannedMeal m : meals) {
            assertThat(m.namesOf(MenuCategory.STAPLE_FOOD)).hasSize(1);
            assertThat(m.namesOf(MenuCategory.SOUP)).hasSize(1);
            assertThat(m.namesOf(MenuCategory.MAIN_DISH)).hasSize(1);
            assertThat(m.namesOf(MenuCategory.SIDE_DISH)).hasSize(1);
            assertThat(m.namesOf(MenuCategory.KIMCHI)).hasSize(1);
        }
    }

    @Test
    void side_dish_count_two_produces_two_sides() {
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 1), List.of(MealType.LUNCH), richPool(), 2);

        assertThat(meals.get(0).namesOf(MenuCategory.SIDE_DISH)).hasSize(2);
    }

    @Test
    void no_same_menu_twice_in_same_day_across_meals() {
        List<PlannedMeal> meals = generator.generate(
                juneDates(1, 5),
                List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
                richPool(), 1);

        // 같은 날 끼니 간 중복이 없어야 함 (validator가 검출)
        assertThat(validator.validate(meals)).isEmpty();
    }
}
