package com.mealplan.mealplan.application.usecase;

import com.mealplan.mealplan.domain.entity.MealPlan;
import java.util.List;

/**
 * 저장된 식단표를 조회하는 인바운드 포트 (UseCase).
 *
 * <p>구현: {@code application/usecase/service/GetMealPlanService}.
 * 호출: {@code primary/rest/MealPlanController}.
 */
public interface GetMealPlanUseCase {

    /**
     * ID로 단일 식단표를 조회한다.
     *
     * @param id 식단표 ID
     * @return 식단표
     * @throws com.mealplan.mealplan.domain.exception.MealPlanNotFoundException 존재하지 않을 때
     */
    MealPlan getById(String id);

    /**
     * 저장된 모든 식단표를 import 최신순으로 조회한다.
     */
    List<MealPlan> getAll();
}
