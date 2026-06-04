package com.mealplan.mealplan.application.usecase;

import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.util.List;

/**
 * 저장된 메뉴 풀을 바탕으로 식단을 생성하는 인바운드 포트 (UseCase).
 */
public interface GenerateMealPlanUseCase {

    /**
     * 요청 조건에 맞춰 한 달치(또는 지정 범위) 식단을 생성한다.
     *
     * @param command 생성 조건
     * @return 생성 결과
     * @throws com.mealplan.mealplan.domain.exception.NotMealPlanContentException
     *         저장된 메뉴 풀이 비어 생성할 수 없을 때
     */
    Result generate(GenerateMealPlanCommand command);

    /**
     * 생성 결과.
     *
     * @param meals          생성된 끼니 목록
     * @param ruleViolations 규칙 위반 메시지(검증 결과). 비어 있으면 규칙 완전 준수
     * @param usedAi         AI 생성 사용 여부 (false면 결정론적 생성기로 fallback)
     */
    record Result(List<PlannedMeal> meals, List<String> ruleViolations, boolean usedAi) {
    }
}
