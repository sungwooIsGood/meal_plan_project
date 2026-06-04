package com.mealplan.mealplan.application.output;

import com.mealplan.mealplan.domain.entity.MenuItem;
import java.util.List;

/**
 * 메뉴 텍스트 토큰을 7개 카테고리로 분류하는 Output Port (AI/규칙 기반 구현).
 *
 * <p>구현체: {@code secondary/ai/RuleBasedMenuClassifier}(기본), {@code secondary/ai/OpenAiMenuClassifier}.
 */
public interface MenuClassifierPort {

    /**
     * 텍스트 토큰들을 분류된 메뉴 항목으로 변환한다.
     *
     * <p>급식 메뉴로 판단되지 않는 토큰은 결과에서 제외될 수 있다.
     * 따라서 반환 목록이 비어 있을 수 있으며, 그 판단(식단표 여부)은 도메인(MealPlan)에서 수행한다.
     *
     * @param tokens 추출된 원시 텍스트 토큰
     * @return 분류된 메뉴 항목 목록 (분류 불가 토큰 제외)
     */
    List<MenuItem> classify(List<String> tokens);
}
