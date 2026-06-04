package com.mealplan.mealplan.domain.entity;

import java.time.LocalDate;
import java.util.List;

/**
 * 생성된 식단의 "하루-한끼" 단위 (Value Object).
 *
 * @param date     날짜
 * @param mealType 끼니
 * @param items    이 끼니에 배정된 메뉴들 (카테고리 포함)
 */
public record PlannedMeal(LocalDate date, MealType mealType, List<MenuItem> items) {

    public PlannedMeal {
        if (date == null) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }
        if (mealType == null) {
            throw new IllegalArgumentException("끼니는 필수입니다.");
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** 해당 카테고리의 메뉴 이름들. */
    public List<String> namesOf(MenuCategory category) {
        return items.stream()
                .filter(i -> i.category() == category)
                .map(MenuItem::name)
                .toList();
    }
}
