package com.mealplan.mealplan.domain.entity;

import java.util.Objects;
import java.util.UUID;

/**
 * MealPlan Aggregate 식별자 (Value Object).
 */
public final class MealPlanId {

    private final String value;

    private MealPlanId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MealPlanId 값은 비어 있을 수 없습니다.");
        }
        this.value = value;
    }

    public static MealPlanId of(String value) {
        return new MealPlanId(value);
    }

    public static MealPlanId newId() {
        return new MealPlanId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MealPlanId other)) {
            return false;
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
