package com.mealplan.mealplan.application.usecase.service;

import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.usecase.GetMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.exception.MealPlanNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 식단표 조회 UseCase 구현.
 */
@Service
public class GetMealPlanService implements GetMealPlanUseCase {

    private final MealPlanRepositoryPort repository;

    public GetMealPlanService(MealPlanRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public MealPlan getById(String id) {
        return repository.findById(MealPlanId.of(id))
                .orElseThrow(() -> new MealPlanNotFoundException(id));
    }

    @Override
    public List<MealPlan> getAll() {
        return repository.findAll();
    }
}
