package com.mealplan.mealplan.application.usecase;

import com.mealplan.mealplan.domain.entity.MealPlan;

/**
 * 엑셀 파일을 받아 식단표를 import 하는 인바운드 포트 (UseCase).
 *
 * <p>구현: {@code application/usecase/service/ImportMealPlanService}.
 * 호출: {@code primary/rest/MealPlanController}.
 */
public interface ImportMealPlanUseCase {

    /**
     * 엑셀 파일 바이트를 분석·분류하여 식단표로 저장한다.
     *
     * @param command 업로드된 파일 정보
     * @return 저장된 MealPlan
     * @throws com.mealplan.mealplan.domain.exception.InvalidExcelFileException 파일이 .xlsx가 아니거나 읽을 수 없을 때
     * @throws com.mealplan.mealplan.domain.exception.NotMealPlanContentException 급식 내용이 없을 때
     */
    MealPlan importMealPlan(ImportMealPlanCommand command);
}
