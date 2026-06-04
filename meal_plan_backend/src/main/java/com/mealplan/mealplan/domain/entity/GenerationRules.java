package com.mealplan.mealplan.domain.entity;

import java.util.List;
import java.util.Map;

/**
 * 식단 생성 기본 규칙 (단일 출처).
 *
 * <p>사용자 요구사항(requirements)이 없을 때 적용되는 규칙이며,
 * 프론트엔드 안내 문구로도 이 값을 그대로 노출한다.
 *
 * <p>중복 규칙은 "조식·중식·석식을 합친 전체 타임라인 기준, N일 이내 동일 메뉴 금지"이다.
 * 끼니가 달라도 동일하게 적용된다. 예) 밥류 7 → 1일 조식에 흰쌀밥이 나오면 7일 이내 어떤 끼니에도 흰쌀밥 금지.
 */
public final class GenerationRules {

    private GenerationRules() {
    }

    /** 하루 한 끼 구성 설명. */
    public static final List<String> DAILY_COMPOSITION = List.of(
            "주식(STAPLE_FOOD) 1개",
            "국/탕(SOUP) 1개",
            "주찬(MAIN_DISH) 1개",
            "부찬(SIDE_DISH) 1개 또는 2개",
            "김치(KIMCHI) 1개",
            "후식(DESSERT)/음료(BEVERAGE)는 선택"
    );

    /**
     * 카테고리별 "끼니 통합 전체 타임라인 내 동일 메뉴 금지" 일수. 0 또는 미포함이면 제한 없음.
     *
     * <p>주식(STAPLE_FOOD)은 메뉴에 따라 세분화된다 — {@link #repeatWindowDays(MenuItem)} 참고.
     * 이 맵의 STAPLE_FOOD 값(7)은 "일반 곡물밥"의 기본값이다.
     */
    public static final Map<MenuCategory, Integer> NO_REPEAT_WITHIN_DAYS = Map.of(
            MenuCategory.STAPLE_FOOD, 7,    // 주식(일반 곡물밥): 7일. 일품 주식은 30일(아래 참고)
            MenuCategory.KIMCHI, 4,        // 김치류: 4일 내 동일 금지
            MenuCategory.SIDE_DISH, 14,    // 부찬류: 14일(2주) 내 동일 금지
            MenuCategory.SOUP, 7,          // 국/탕: 7일 내 동일 금지
            MenuCategory.MAIN_DISH, 30     // 주찬: 한 달 내 최대한 중복 회피
    );

    /** 주식 세부 규칙: 일반 곡물밥(7일)과 일품 주식(30일)을 구분하는 일수. */
    public static final int SIMPLE_RICE_REPEAT_DAYS = 7;
    public static final int SPECIAL_STAPLE_REPEAT_DAYS = 30;

    /**
     * 해당 메뉴 항목의 중복 금지 일수(끼니 통합 기준)를 반환한다. 0이면 제한 없음.
     *
     * <p>주식은 {@link MenuItem#isSimpleRice()} 기준으로 나뉜다.
     * - 일반 곡물밥(RICE) → 7일
     * - 일품 주식(DISH) → 30일
     * 그 외 카테고리는 {@link #NO_REPEAT_WITHIN_DAYS} 값을 따른다.
     */
    public static int repeatWindowDays(MenuItem item) {
        if (item.category() == MenuCategory.STAPLE_FOOD) {
            return item.isSimpleRice() ? SIMPLE_RICE_REPEAT_DAYS : SPECIAL_STAPLE_REPEAT_DAYS;
        }
        Integer days = NO_REPEAT_WITHIN_DAYS.get(item.category());
        return days == null ? 0 : days;
    }

    /** 끼니 간 규칙 설명. */
    public static final List<String> CROSS_MEAL_RULES = List.of(
            "중복 금지 기간은 조식·중식·석식을 합친 전체 타임라인 기준으로 적용된다 (끼니가 달라도 동일).",
            "같은 날 안에서는 끼니 간 메뉴가 겹치지 않는다 (조식에 쓴 메뉴는 그날 중식·석식에 나오지 않음)."
    );

    // ─────────────────────────────────────────────────────────────────────────
    // 후보 풀(Candidate Pool) 정책
    // 저장된 전체 메뉴를 AI/생성기에 다 주지 않고, 카테고리별로 후보 N종만 추려서 전달한다.
    // 전국 규모로 데이터가 커져도 프롬프트(토큰) 크기를 일정하게 유지하기 위함.
    // 추출 시점: 생성 요청 시(read-time). MongoDB aggregation으로 DB가 집계한다.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 후보 풀에서 "인기 상위(빈도순)"가 차지하는 비율. 나머지는 랜덤 샘플.
     * 예) 0.6 → 후보의 60%는 빈도 상위, 40%는 (상위 제외) 무작위.
     * 인기로 품질을 깔고 랜덤으로 다양성·롱테일을 확보한다.
     */
    public static final double POPULAR_RATIO = 0.6;

    /**
     * 카테고리별 후보 개수(N). 조식·중식·석식 3끼 + 주말 포함(최대 31일×3끼=93배치)까지
     * 고려해 중복 금지 기간을 최대한 지킬 수 있도록 넉넉히 잡는다.
     * 풀의 고유 메뉴가 N보다 적으면 있는 것 전부 사용한다.
     *
     * <p>총합(약 420종)이 AI에 전달되는 후보 상한이다. 메뉴명이 짧아 토큰 부담은 작다.
     */
    public static final Map<MenuCategory, Integer> CANDIDATE_POOL_SIZE = Map.of(
            MenuCategory.MAIN_DISH, 90,    // 주찬: 30일 윈도우 × 다끼니 대비
            MenuCategory.SIDE_DISH, 120,   // 부찬: 하루 1~2개 + 14일 윈도우 → 가장 많이 필요
            MenuCategory.SOUP, 60,         // 국/탕: 7일 윈도우
            MenuCategory.STAPLE_FOOD, 50,  // 주식: 7일 윈도우(밥 종류 한정적)
            MenuCategory.KIMCHI, 30,       // 김치: 4일 윈도우(종류 적음)
            MenuCategory.DESSERT, 40,      // 후식: 선택 항목
            MenuCategory.BEVERAGE, 30      // 음료: 선택 항목
    );

    /** 후보 N종 중 인기 상위로 채울 개수. */
    public static int popularCount(MenuCategory category) {
        int n = CANDIDATE_POOL_SIZE.getOrDefault(category, 0);
        return (int) Math.round(n * POPULAR_RATIO);
    }

    /** 후보 N종 중 랜덤으로 채울 개수. */
    public static int randomCount(MenuCategory category) {
        int n = CANDIDATE_POOL_SIZE.getOrDefault(category, 0);
        return n - popularCount(category);
    }
}
