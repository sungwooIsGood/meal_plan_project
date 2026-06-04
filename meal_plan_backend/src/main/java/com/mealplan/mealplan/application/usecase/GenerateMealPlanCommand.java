package com.mealplan.mealplan.application.usecase;

import com.mealplan.mealplan.domain.entity.MealType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 식단 생성 요청 커맨드.
 *
 * @param yearMonth        대상 년월
 * @param startDay         시작일(1~말일). null이면 1일
 * @param endDay           종료일(1~말일). null이면 말일
 * @param mealTypes        생성할 끼니들 (비면 조식만)
 * @param requirements     사용자 자유 요구사항 (null/blank면 기본 규칙 적용)
 * @param mustIncludeMenus 꼭 포함할 메뉴명들 (null이면 빈 목록)
 * @param includeWeekends  주말(토·일) 포함 여부. null이면 true(포함)
 */
public record GenerateMealPlanCommand(
        YearMonth yearMonth,
        Integer startDay,
        Integer endDay,
        List<MealType> mealTypes,
        String requirements,
        List<String> mustIncludeMenus,
        Boolean includeWeekends) {

    public GenerateMealPlanCommand {
        if (yearMonth == null) {
            throw new IllegalArgumentException("년월은 필수입니다.");
        }
        int last = yearMonth.lengthOfMonth();
        int s = startDay == null ? 1 : startDay;
        int e = endDay == null ? last : endDay;
        if (s < 1 || s > last || e < 1 || e > last) {
            throw new IllegalArgumentException(
                    "날짜 범위가 잘못되었습니다. " + yearMonth + "는 1~" + last + "일까지입니다.");
        }
        if (s > e) {
            throw new IllegalArgumentException("시작일이 종료일보다 클 수 없습니다.");
        }
        startDay = s;
        endDay = e;
        mealTypes = (mealTypes == null || mealTypes.isEmpty())
                ? List.of(MealType.BREAKFAST)
                : List.copyOf(mealTypes);
        mustIncludeMenus = mustIncludeMenus == null ? List.of() : List.copyOf(mustIncludeMenus);
        includeWeekends = includeWeekends == null ? Boolean.TRUE : includeWeekends;
    }

    public boolean hasRequirements() {
        return requirements != null && !requirements.isBlank();
    }

    /**
     * 생성 대상 날짜 목록.
     * {@code includeWeekends}가 false면 토·일을 제외한 평일만 반환한다.
     */
    public List<LocalDate> targetDates() {
        return java.util.stream.IntStream.rangeClosed(startDay, endDay)
                .mapToObj(yearMonth::atDay)
                .filter(date -> includeWeekends || !isWeekend(date))
                .toList();
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
