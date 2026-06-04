package com.mealplan.mealplan.primary.rest.dto;

import com.mealplan.mealplan.application.usecase.GenerateMealPlanCommand;
import com.mealplan.mealplan.domain.entity.MealType;
import java.time.YearMonth;
import java.util.List;

/**
 * 식단 생성 요청 body. 모든 필드 선택적.
 *
 * @param startDay         시작일(없으면 1일)
 * @param endDay           종료일(없으면 말일)
 * @param mealTypes        끼니 목록(없으면 조식)
 * @param requirements     자유 요구사항(없으면 기본 규칙)
 * @param mustIncludeMenus 꼭 포함할 메뉴
 * @param includeWeekends  주말 포함 여부(없으면 true=포함)
 */
public record GenerateMealPlanRequest(
        Integer startDay,
        Integer endDay,
        List<MealType> mealTypes,
        String requirements,
        List<String> mustIncludeMenus,
        Boolean includeWeekends) {

    public GenerateMealPlanCommand toCommand(YearMonth yearMonth) {
        return new GenerateMealPlanCommand(
                yearMonth, startDay, endDay, mealTypes, requirements, mustIncludeMenus,
                includeWeekends);
    }
}
