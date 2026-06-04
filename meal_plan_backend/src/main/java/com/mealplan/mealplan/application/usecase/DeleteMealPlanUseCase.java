package com.mealplan.mealplan.application.usecase;

import java.util.List;

/**
 * 저장된 식단표를 삭제하는 인바운드 포트 (UseCase).
 */
public interface DeleteMealPlanUseCase {

    /**
     * 주어진 ID들의 식단표를 삭제한다.
     *
     * @param ids 삭제할 식단표 ID 목록
     * @return 실제로 삭제된 개수
     */
    long deleteByIds(List<String> ids);
}
