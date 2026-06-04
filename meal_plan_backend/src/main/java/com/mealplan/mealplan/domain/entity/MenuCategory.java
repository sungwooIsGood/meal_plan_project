package com.mealplan.mealplan.domain.entity;

/**
 * 급식 메뉴 분류. 사용자 스펙에 정의된 7개 카테고리.
 *
 * <ul>
 *   <li>STAPLE_FOOD - 주식: 탄수화물 중심 기본 음식 (쌀밥, 잡곡밥, 볶음밥, 국수)</li>
 *   <li>SOUP - 국/탕: 국물 요리 (미역국, 된장국, 육개장)</li>
 *   <li>MAIN_DISH - 주찬/메인: 단백질 중심 주요 반찬 (제육볶음, 돈가스, 닭갈비)</li>
 *   <li>SIDE_DISH - 부찬/사이드: 보조 반찬 (멸치볶음, 시금치나물, 계란말이)</li>
 *   <li>KIMCHI - 김치: 배추김치, 깍두기</li>
 *   <li>DESSERT - 후식: 과일, 요구르트, 떡</li>
 *   <li>BEVERAGE - 음료: 우유, 주스</li>
 * </ul>
 */
public enum MenuCategory {
    STAPLE_FOOD("주식"),
    SOUP("국/탕"),
    MAIN_DISH("주찬"),
    SIDE_DISH("부찬"),
    KIMCHI("김치"),
    DESSERT("후식"),
    BEVERAGE("음료");

    private final String koreanName;

    MenuCategory(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
