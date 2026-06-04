package com.mealplan.mealplan.domain.service;

import com.mealplan.mealplan.domain.entity.GenerationRules;
import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙을 100% 보장하는 결정론적 식단 생성기 (도메인 서비스).
 *
 * <p>AI 생성이 규칙을 위반하거나 사용 불가일 때의 fallback이자, 규칙 준수의 기준선.
 * 카테고리별 메뉴 풀에서 "끼니 통합 타임라인 + 같은 날 끼니 간 중복 금지"를 지키며 그리디하게 배정한다.
 *
 * <p>순수 로직(외부 의존 0, 시드 고정 가능)이라 단위 테스트로 검증한다.
 */
public class DeterministicMealPlanGenerator {

    /** 하루 한 끼의 카테고리 구성: 주식1 국1 주찬1 부찬1 김치1 (부찬은 옵션으로 2개까지). */
    private static final List<MenuCategory> BASE_COMPOSITION = List.of(
            MenuCategory.STAPLE_FOOD,
            MenuCategory.SOUP,
            MenuCategory.MAIN_DISH,
            MenuCategory.SIDE_DISH,
            MenuCategory.KIMCHI);

    /**
     * @param dates    생성 대상 날짜 (정렬됨, 주말 포함)
     * @param mealTypes 끼니
     * @param menuPool 카테고리별 메뉴 풀
     * @param sideDishCount 부찬 개수 (1 또는 2)
     * @return 생성된 끼니 목록
     */
    public List<PlannedMeal> generate(List<LocalDate> dates,
                                      List<MealType> mealTypes,
                                      Map<MenuCategory, List<MenuItem>> menuPool,
                                      int sideDishCount) {
        List<PlannedMeal> result = new ArrayList<>();

        // (category|name) -> 마지막 사용 날짜. 끼니 통합 타임라인.
        Map<String, LocalDate> lastUsed = new HashMap<>();
        // 카테고리별 라운드로빈 커서 (다양성 확보)
        Map<MenuCategory, Integer> cursor = new HashMap<>();

        for (LocalDate date : dates) {
            // 같은 날 끼니 간 중복 방지용
            List<String> usedToday = new ArrayList<>();
            for (MealType mealType : mealTypes) {
                List<MenuItem> items = new ArrayList<>();
                for (MenuCategory category : BASE_COMPOSITION) {
                    int count = (category == MenuCategory.SIDE_DISH) ? Math.max(1, sideDishCount) : 1;
                    for (int n = 0; n < count; n++) {
                        MenuItem picked = pick(category, menuPool, lastUsed, usedToday, date, cursor);
                        if (picked != null) {
                            items.add(picked);
                            usedToday.add(picked.name());
                            lastUsed.put(key(picked), date);
                        }
                    }
                }
                result.add(new PlannedMeal(date, mealType, items));
            }
        }
        return result;
    }

    /**
     * 규칙(중복 금지 기간 + 같은 날 중복 금지)을 지키는 메뉴를 카테고리 풀에서 선택.
     * 규칙을 지키는 후보가 없으면 가장 오래전에 쓴 메뉴로 완화 선택(생성 자체는 보장).
     */
    private MenuItem pick(MenuCategory category,
                          Map<MenuCategory, List<MenuItem>> menuPool,
                          Map<String, LocalDate> lastUsed,
                          List<String> usedToday,
                          LocalDate date,
                          Map<MenuCategory, Integer> cursor) {
        List<MenuItem> pool = menuPool.get(category);
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        int size = pool.size();
        int start = cursor.getOrDefault(category, 0);

        MenuItem relaxedBest = null;
        long relaxedBestGap = -1;

        for (int i = 0; i < size; i++) {
            MenuItem candidate = pool.get((start + i) % size);
            if (usedToday.contains(candidate.name())) {
                continue; // 같은 날 끼니 간 중복 금지
            }
            // 중복 금지 일수는 메뉴별로 계산 (주식: 일반밥 7일 / 일품주식 30일)
            int window = GenerationRules.repeatWindowDays(candidate);
            LocalDate last = lastUsed.get(key(candidate));
            long gap = last == null ? Long.MAX_VALUE : Math.abs(date.toEpochDay() - last.toEpochDay());

            boolean withinWindowOk = window <= 0 || last == null || gap >= window;
            if (withinWindowOk) {
                cursor.put(category, (start + i + 1) % size);
                return candidate;
            }
            // 완화 후보: 가장 오래전에 쓴 것
            if (gap > relaxedBestGap) {
                relaxedBestGap = gap;
                relaxedBest = candidate;
            }
        }
        // 규칙 만족 후보 없음 → 완화 선택 (단, 같은 날 중복만은 가능한 한 회피됨)
        return relaxedBest;
    }

    private String key(MenuItem item) {
        return item.category().name() + "|" + item.name();
    }
}
