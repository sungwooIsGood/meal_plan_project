package com.mealplan.mealplan.domain.entity;

/**
 * 주식(STAPLE_FOOD)의 세부 유형. 중복 금지 규칙 차등 적용에만 사용된다(화면 노출 X).
 *
 * <ul>
 *   <li>{@link #RICE} - 일반 곡물밥: 흰쌀밥·현미밥·흑미밥·잡곡밥 등. 자주 나와도 무방 → 7일 규칙.</li>
 *   <li>{@link #DISH} - 일품 주식: 볶음밥·비빔밥·콩나물밥·덮밥·국수·파스타·빵 등 한 끼 자체가 되는 주식 → 30일 규칙.</li>
 * </ul>
 *
 * <p>주식이 아닌 카테고리에는 적용되지 않는다(null).
 */
public enum StapleType {
    RICE,
    DISH
}
