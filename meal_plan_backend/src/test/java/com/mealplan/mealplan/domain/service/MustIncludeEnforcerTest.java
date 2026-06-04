package com.mealplan.mealplan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MustIncludeEnforcerTest {

    private final MustIncludeEnforcer enforcer = new MustIncludeEnforcer();

    private List<PlannedMeal> oneMeal(MenuItem... items) {
        return List.of(new PlannedMeal(LocalDate.of(2026, 6, 1), MealType.BREAKFAST, List.of(items)));
    }

    private List<String> allNames(List<PlannedMeal> meals) {
        return meals.stream()
                .flatMap(m -> m.items().stream())
                .map(MenuItem::name)
                .toList();
    }

    @Test
    void adds_must_include_menu_even_if_not_in_pool() {
        // 풀에 없는 "돈가스덮밥"을 강제 포함 → 결과에 반드시 들어감
        List<PlannedMeal> meals = oneMeal(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD));

        List<PlannedMeal> result = enforcer.enforce(meals, List.of("돈가스덮밥"), Map.of());

        assertThat(allNames(result)).contains("돈가스덮밥");
    }

    @Test
    void infers_category_from_name_when_unknown() {
        // "돈가스덮밥"은 '밥' 포함 → STAPLE_FOOD로 추론
        List<PlannedMeal> result = enforcer.enforce(
                oneMeal(MenuItem.of("미역국", MenuCategory.SOUP)),
                List.of("돈가스덮밥"),
                Map.of());

        MenuItem added = result.get(0).items().stream()
                .filter(i -> i.name().equals("돈가스덮밥"))
                .findFirst()
                .orElseThrow();
        assertThat(added.category()).isEqualTo(MenuCategory.STAPLE_FOOD);
    }

    @Test
    void uses_known_category_from_pool_when_available() {
        List<PlannedMeal> result = enforcer.enforce(
                oneMeal(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD)),
                List.of("제육볶음"),
                Map.of("제육볶음", MenuCategory.MAIN_DISH));

        MenuItem added = result.get(0).items().stream()
                .filter(i -> i.name().equals("제육볶음"))
                .findFirst()
                .orElseThrow();
        assertThat(added.category()).isEqualTo(MenuCategory.MAIN_DISH);
    }

    @Test
    void does_not_duplicate_when_already_present() {
        List<PlannedMeal> meals = oneMeal(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("제육볶음", MenuCategory.MAIN_DISH));

        List<PlannedMeal> result = enforcer.enforce(meals, List.of("제육볶음"), Map.of());

        long count = allNames(result).stream().filter("제육볶음"::equals).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void empty_must_include_returns_unchanged() {
        List<PlannedMeal> meals = oneMeal(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD));
        assertThat(enforcer.enforce(meals, List.of(), Map.of())).isEqualTo(meals);
    }

    @Test
    void infers_kimchi_and_beverage_and_soup() {
        List<PlannedMeal> result = enforcer.enforce(
                oneMeal(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD)),
                List.of("총각김치", "딸기우유", "순두부찌개"),
                Map.of());

        Map<String, MenuCategory> byName = result.get(0).items().stream()
                .collect(java.util.stream.Collectors.toMap(MenuItem::name, MenuItem::category, (a, b) -> a));
        assertThat(byName.get("총각김치")).isEqualTo(MenuCategory.KIMCHI);
        assertThat(byName.get("딸기우유")).isEqualTo(MenuCategory.BEVERAGE);
        assertThat(byName.get("순두부찌개")).isEqualTo(MenuCategory.SOUP);
    }

    // --- 분산 배치 / 공백 정규화 ---

    private List<PlannedMeal> emptyMeals(int count) {
        List<PlannedMeal> meals = new java.util.ArrayList<>();
        for (int d = 1; d <= count; d++) {
            meals.add(new PlannedMeal(LocalDate.of(2026, 6, d), MealType.BREAKFAST, List.of()));
        }
        return meals;
    }

    @Test
    void distributes_must_include_across_different_meals() {
        // 끼니(날짜) 10개에 2개 강제 → 같은 끼니에 몰리지 않고 서로 다른 끼니에 배치
        List<PlannedMeal> meals = emptyMeals(10);

        List<PlannedMeal> result = enforcer.enforce(meals, List.of("돈가스", "돈가스덮밥"), Map.of());

        // 두 메뉴가 들어간 끼니 인덱스를 찾는다
        int idxDonkatsu = indexOfMealContaining(result, "돈가스");
        int idxDeopbap = indexOfMealContaining(result, "돈가스덮밥");
        assertThat(idxDonkatsu).isGreaterThanOrEqualTo(0);
        assertThat(idxDeopbap).isGreaterThanOrEqualTo(0);
        assertThat(idxDonkatsu).isNotEqualTo(idxDeopbap); // 서로 다른 끼니
    }

    @Test
    void treats_whitespace_variants_as_same_menu() {
        // 이미 "돈가스덮밥"이 있는데 "돈가스 덮밥"(공백)을 요청 → 중복으로 보고 추가 안 함
        List<PlannedMeal> meals = oneMeal(MenuItem.of("돈가스덮밥", MenuCategory.STAPLE_FOOD));

        List<PlannedMeal> result = enforcer.enforce(meals, List.of("돈가스 덮밥"), Map.of());

        long count = allNames(result).stream()
                .filter(n -> n.replaceAll("\\s+", "").equals("돈가스덮밥"))
                .count();
        assertThat(count).isEqualTo(1); // 공백만 다른 중복은 추가되지 않음
    }

    private int indexOfMealContaining(List<PlannedMeal> meals, String name) {
        for (int i = 0; i < meals.size(); i++) {
            boolean has = meals.get(i).items().stream().anyMatch(it -> it.name().equals(name));
            if (has) {
                return i;
            }
        }
        return -1;
    }
}
