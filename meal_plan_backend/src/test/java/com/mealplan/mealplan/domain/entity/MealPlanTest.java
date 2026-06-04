package com.mealplan.mealplan.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealPlanTest {

    @Test
    void create_builds_meal_plan_with_items() {
        List<MenuItem> items = List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("미역국", MenuCategory.SOUP));

        MealPlan plan = MealPlan.create("2026-06.xlsx", items);

        assertThat(plan.id()).isNotNull();
        assertThat(plan.sourceFileName()).isEqualTo("2026-06.xlsx");
        assertThat(plan.importedAt()).isNotNull();
        assertThat(plan.itemCount()).isEqualTo(2);
    }

    @Test
    void should_throw_NotMealPlanContent_when_no_items() {
        assertThatThrownBy(() -> MealPlan.create("random.xlsx", List.of()))
                .isInstanceOf(NotMealPlanContentException.class)
                .hasMessageContaining("급식");
    }

    @Test
    void should_throw_when_source_file_name_blank() {
        List<MenuItem> items = List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD));

        assertThatThrownBy(() -> MealPlan.create("  ", items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일명");
    }

    @Test
    void itemsByCategory_groups_items() {
        List<MenuItem> items = List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("흑미밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("미역국", MenuCategory.SOUP),
                MenuItem.of("배추김치", MenuCategory.KIMCHI));

        MealPlan plan = MealPlan.create("plan.xlsx", items);

        assertThat(plan.itemsByCategory().get(MenuCategory.STAPLE_FOOD))
                .containsExactly("흰쌀밥", "흑미밥");
        assertThat(plan.itemsByCategory().get(MenuCategory.SOUP))
                .containsExactly("미역국");
        assertThat(plan.itemsByCategory().get(MenuCategory.KIMCHI))
                .containsExactly("배추김치");
    }

    @Test
    void items_list_is_immutable() {
        MealPlan plan = MealPlan.create("plan.xlsx",
                List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD)));

        assertThatThrownBy(() -> plan.items().add(MenuItem.of("국수", MenuCategory.STAPLE_FOOD)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void restore_rebuilds_plan_with_given_id_and_time() {
        MealPlanId id = MealPlanId.of("fixed-id");
        Instant when = Instant.parse("2026-06-01T00:00:00Z");
        List<MenuItem> items = List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD));

        MealPlan plan = MealPlan.restore(id, "plan.xlsx", when, items);

        assertThat(plan.id()).isEqualTo(id);
        assertThat(plan.importedAt()).isEqualTo(when);
    }
}
