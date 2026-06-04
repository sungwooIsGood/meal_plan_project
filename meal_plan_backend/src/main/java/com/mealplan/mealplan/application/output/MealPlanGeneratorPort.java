package com.mealplan.mealplan.application.output;

import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import com.mealplan.mealplan.domain.entity.MealType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 메뉴 풀과 조건을 받아 식단을 생성하는 Output Port (AI 구현).
 *
 * <p>구현체: {@code secondary/ai/OpenAiMealPlanGenerator}.
 * 생성 결과는 도메인 검증기로 다시 검증되므로, 이 포트는 "최선의 초안"을 만든다.
 */
public interface MealPlanGeneratorPort {

    /**
     * @param dates            생성 대상 날짜(주말 포함, 정렬됨)
     * @param mealTypes        생성할 끼니
     * @param menuPool         카테고리별 사용 가능한 메뉴 풀
     * @param requirements     사용자 자유 요구사항 (nullable)
     * @param mustIncludeMenus 꼭 포함할 메뉴명
     * @param ruleSummary      준수해야 할 규칙 요약 텍스트
     * @return 생성된 끼니 목록. 생성 불가/실패 시 빈 목록
     */
    List<PlannedMeal> generate(
            List<LocalDate> dates,
            List<MealType> mealTypes,
            Map<MenuCategory, List<MenuItem>> menuPool,
            String requirements,
            List<String> mustIncludeMenus,
            String ruleSummary);
}
