package com.mealplan.mealplan.domain.exception;

/**
 * 주어진 ID의 식단표가 존재하지 않을 때 발생.
 *
 * <p>웹 어댑터에서 404(Not Found)로 매핑된다.
 */
public class MealPlanNotFoundException extends RuntimeException {

    public MealPlanNotFoundException(String id) {
        super("식단표를 찾을 수 없습니다. id=" + id);
    }
}
