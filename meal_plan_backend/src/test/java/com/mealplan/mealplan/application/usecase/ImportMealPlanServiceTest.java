package com.mealplan.mealplan.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mealplan.mealplan.application.output.MealPlanFileReaderPort;
import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.output.MenuClassifierPort;
import com.mealplan.mealplan.application.usecase.service.ImportMealPlanService;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ImportMealPlanServiceTest {

    private static final byte[] ANY_CONTENT = new byte[]{1, 2, 3};

    @Test
    void imports_and_saves_meal_plan_on_happy_path() {
        MealPlanFileReaderPort reader = (name, content) -> List.of("흰쌀밥", "미역국");
        MenuClassifierPort classifier = tokens -> List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("미역국", MenuCategory.SOUP));
        RecordingRepository repository = new RecordingRepository();

        ImportMealPlanService service =
                new ImportMealPlanService(reader, classifier, repository);

        MealPlan result = service.importMealPlan(
                new ImportMealPlanCommand("plan.xlsx", "application/...", ANY_CONTENT));

        assertThat(result.itemCount()).isEqualTo(2);
        assertThat(repository.saved).isNotNull();
        assertThat(repository.saved.sourceFileName()).isEqualTo("plan.xlsx");
    }

    @Test
    void propagates_invalid_excel_from_reader() {
        MealPlanFileReaderPort reader = (name, content) -> {
            throw new InvalidExcelFileException("not xlsx");
        };
        MenuClassifierPort classifier = tokens -> List.of();
        RecordingRepository repository = new RecordingRepository();

        ImportMealPlanService service =
                new ImportMealPlanService(reader, classifier, repository);

        assertThatThrownBy(() -> service.importMealPlan(
                new ImportMealPlanCommand("doc.xlsx", null, ANY_CONTENT)))
                .isInstanceOf(InvalidExcelFileException.class);
        assertThat(repository.saved).isNull();
    }

    @Test
    void throws_NotMealPlanContent_when_classifier_returns_empty() {
        MealPlanFileReaderPort reader = (name, content) -> List.of("랜덤", "텍스트");
        MenuClassifierPort classifier = tokens -> List.of();
        RecordingRepository repository = new RecordingRepository();

        ImportMealPlanService service =
                new ImportMealPlanService(reader, classifier, repository);

        assertThatThrownBy(() -> service.importMealPlan(
                new ImportMealPlanCommand("notmeal.xlsx", null, ANY_CONTENT)))
                .isInstanceOf(NotMealPlanContentException.class);
        assertThat(repository.saved).isNull();
    }

    private static final class RecordingRepository implements MealPlanRepositoryPort {
        private MealPlan saved;

        @Override
        public MealPlan save(MealPlan mealPlan) {
            this.saved = mealPlan;
            return mealPlan;
        }

        @Override
        public Optional<MealPlan> findById(MealPlanId id) {
            return Optional.ofNullable(saved);
        }

        @Override
        public java.util.List<MealPlan> findAll() {
            return saved == null ? java.util.List.of() : java.util.List.of(saved);
        }

        @Override
        public long deleteByIds(java.util.List<MealPlanId> ids) {
            return 0L;
        }

        @Override
        public java.util.Map<com.mealplan.mealplan.domain.entity.MenuCategory,
                java.util.List<MenuItem>> loadCandidatePool() {
            return java.util.Map.of();
        }
    }
}
