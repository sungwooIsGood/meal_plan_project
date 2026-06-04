package com.mealplan.mealplan.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.usecase.service.GetMealPlanService;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.exception.MealPlanNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetMealPlanServiceTest {

    private MealPlan sample(String id) {
        return MealPlan.restore(
                MealPlanId.of(id),
                "2026-06.xlsx",
                Instant.parse("2026-06-01T00:00:00Z"),
                List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD)));
    }

    @Test
    void getById_returns_meal_plan_when_present() {
        MealPlan stored = sample("abc");
        GetMealPlanService service = new GetMealPlanService(new StubRepository(stored));

        MealPlan result = service.getById("abc");

        assertThat(result.id().value()).isEqualTo("abc");
        assertThat(result.sourceFileName()).isEqualTo("2026-06.xlsx");
    }

    @Test
    void getById_throws_NotFound_when_absent() {
        GetMealPlanService service = new GetMealPlanService(new StubRepository(null));

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(MealPlanNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getAll_returns_all_from_repository() {
        GetMealPlanService service = new GetMealPlanService(new StubRepository(sample("abc")));

        List<MealPlan> all = service.getAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).id().value()).isEqualTo("abc");
    }

    /** id가 일치할 때만 반환하는 단순 스텁. */
    private static final class StubRepository implements MealPlanRepositoryPort {
        private final MealPlan stored;

        StubRepository(MealPlan stored) {
            this.stored = stored;
        }

        @Override
        public MealPlan save(MealPlan mealPlan) {
            return mealPlan;
        }

        @Override
        public Optional<MealPlan> findById(MealPlanId id) {
            if (stored != null && stored.id().equals(id)) {
                return Optional.of(stored);
            }
            return Optional.empty();
        }

        @Override
        public List<MealPlan> findAll() {
            return stored == null ? List.of() : List.of(stored);
        }

        @Override
        public long deleteByIds(List<MealPlanId> ids) {
            return 0L;
        }

        @Override
        public java.util.Map<com.mealplan.mealplan.domain.entity.MenuCategory,
                List<com.mealplan.mealplan.domain.entity.MenuItem>> loadCandidatePool() {
            return java.util.Map.of();
        }
    }
}
