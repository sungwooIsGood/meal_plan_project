package com.mealplan.mealplan.domain.entity;

/**
 * 끼니 구분.
 */
public enum MealType {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식");

    private final String koreanName;

    MealType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
