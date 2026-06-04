package com.mealplan.mealplan.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mealplan.mealplan.domain.entity.MealType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateMealPlanCommandTest {

    private GenerateMealPlanCommand cmd(Integer start, Integer end, Boolean includeWeekends) {
        return new GenerateMealPlanCommand(
                YearMonth.of(2026, 6), start, end, List.of(MealType.BREAKFAST),
                null, null, includeWeekends);
    }

    @Test
    void includeWeekends_null_defaults_to_including_weekends() {
        // 2026-06: 1일(월)~7일(일) → 7일 전부 포함
        List<LocalDate> dates = cmd(1, 7, null).targetDates();
        assertThat(dates).hasSize(7);
    }

    @Test
    void includeWeekends_true_includes_saturday_and_sunday() {
        List<LocalDate> dates = cmd(1, 7, true).targetDates();
        assertThat(dates).hasSize(7);
        assertThat(dates).anyMatch(d -> d.getDayOfWeek() == DayOfWeek.SATURDAY);
        assertThat(dates).anyMatch(d -> d.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    @Test
    void includeWeekends_false_excludes_weekends() {
        // 2026-06-01(월)~07(일) 중 평일(월~금)만 = 1,2,3,4,5일 → 5일
        List<LocalDate> dates = cmd(1, 7, false).targetDates();
        assertThat(dates).hasSize(5);
        assertThat(dates).noneMatch(d ->
                d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY);
    }

    @Test
    void rejects_day_out_of_month_range() {
        assertThatThrownBy(() -> cmd(1, 31, true)) // 6월은 30일까지 → 31 invalid
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_start_after_end() {
        assertThatThrownBy(() -> cmd(10, 5, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
