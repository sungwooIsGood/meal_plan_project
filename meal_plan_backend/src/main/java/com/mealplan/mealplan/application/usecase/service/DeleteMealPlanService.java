package com.mealplan.mealplan.application.usecase.service;

import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.usecase.DeleteMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 식단표 삭제 UseCase 구현.
 */
@Service
public class DeleteMealPlanService implements DeleteMealPlanUseCase {

    private final MealPlanRepositoryPort repository;

    public DeleteMealPlanService(MealPlanRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public long deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        List<MealPlanId> mealPlanIds = ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(MealPlanId::of)
                .toList();
        return repository.deleteByIds(mealPlanIds);
    }
}
