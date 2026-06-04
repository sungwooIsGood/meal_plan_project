package com.mealplan.mealplan.domain.service;

import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * "꼭 포함할 메뉴(mustInclude)"가 생성 결과에 반드시 들어가도록 보장하는 도메인 서비스.
 *
 * <p>정책(사용자 요구): mustInclude로 지정된 메뉴는 <b>DB(메뉴 풀)에 없더라도</b> 결과에 강제로 넣는다.
 * 이미 결과에 있으면 그대로 두고, 없으면 첫 번째 끼니에 추가한다.
 *
 * <p>카테고리 결정 순서:
 * <ol>
 *   <li>알려진 카테고리 맵(풀 기준)에 있으면 그 카테고리</li>
 *   <li>없으면 메뉴 이름 키워드로 추론</li>
 *   <li>그래도 모르면 MAIN_DISH(주찬) 기본값</li>
 * </ol>
 *
 * <p>순수 로직(외부 의존 0)이라 단위 테스트로 검증한다.
 */
public class MustIncludeEnforcer {

    /**
     * @param meals          생성된 끼니 목록
     * @param mustInclude    꼭 포함할 메뉴명들
     * @param knownCategories 풀에서 알아낸 (메뉴명 → 카테고리). 풀에 없으면 미포함.
     * @return mustInclude가 모두 포함되도록 보정된 끼니 목록
     */
    public List<PlannedMeal> enforce(List<PlannedMeal> meals,
                                     List<String> mustInclude,
                                     Map<String, MenuCategory> knownCategories) {
        if (mustInclude == null || mustInclude.isEmpty() || meals.isEmpty()) {
            return meals;
        }

        // 이미 결과에 존재하는 메뉴명(정규화 기준) 집합 — 공백/표기 차이를 흡수
        java.util.Set<String> presentNormalized = new java.util.HashSet<>();
        for (PlannedMeal meal : meals) {
            for (MenuItem item : meal.items()) {
                presentNormalized.add(normalize(item.name()));
            }
        }

        // 누락된 mustInclude만 추출 (공백 정규화로 중복/이미존재 판단)
        List<MenuItem> toAdd = new ArrayList<>();
        java.util.Set<String> seenNormalized = new java.util.HashSet<>();
        for (String raw : mustInclude) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            String norm = normalize(name);
            if (name.isEmpty() || norm.isEmpty()) {
                continue;
            }
            // 이미 결과에 (정규화 기준) 있거나, 이번 요청 내 중복이면 스킵
            if (presentNormalized.contains(norm) || !seenNormalized.add(norm)) {
                continue;
            }
            MenuCategory category = resolveCategory(name, knownCategories);
            toAdd.add(MenuItem.of(name, category));
        }
        if (toAdd.isEmpty()) {
            return meals;
        }

        // 누락분을 여러 끼니에 분산해서 추가 (첫 끼니 몰빵 방지).
        // 끼니 수가 추가 항목보다 적으면 라운드로빈으로 돌린다.
        List<PlannedMeal> result = new ArrayList<>(meals);
        List<List<MenuItem>> itemBuckets = new ArrayList<>();
        for (PlannedMeal meal : result) {
            itemBuckets.add(new ArrayList<>(meal.items()));
        }

        int mealCount = result.size();
        // 가능한 한 균등 간격으로 배치할 끼니 인덱스 선택
        for (int i = 0; i < toAdd.size(); i++) {
            int mealIndex = pickSpreadIndex(i, toAdd.size(), mealCount);
            // 같은 끼니에 같은 메뉴(정규화)가 이미 있으면 다음 칸으로 회피
            mealIndex = avoidSameMealDuplicate(toAdd.get(i), mealIndex, itemBuckets);
            itemBuckets.get(mealIndex).add(toAdd.get(i));
        }

        for (int i = 0; i < result.size(); i++) {
            PlannedMeal m = result.get(i);
            result.set(i, new PlannedMeal(m.date(), m.mealType(), itemBuckets.get(i)));
        }
        return result;
    }

    /**
     * 추가 항목 i번째를 전체 끼니에 고르게 분산시킬 인덱스.
     * 예: 끼니 30개에 항목 2개면 0번, 15번 끼니에 배치.
     */
    private int pickSpreadIndex(int itemIndex, int totalItems, int mealCount) {
        if (mealCount <= 1) {
            return 0;
        }
        // (itemIndex + 0.5) / totalItems 비율 위치 → 끼니 인덱스로 매핑 (균등 분산)
        int idx = (int) Math.floor(((itemIndex + 0.5) / Math.max(1, totalItems)) * mealCount);
        return Math.min(mealCount - 1, Math.max(0, idx));
    }

    /** 지정 끼니에 같은 메뉴(정규화)가 이미 있으면, 없는 다음 끼니로 이동. */
    private int avoidSameMealDuplicate(MenuItem item, int startIndex,
                                       List<List<MenuItem>> itemBuckets) {
        int n = itemBuckets.size();
        String norm = normalize(item.name());
        for (int step = 0; step < n; step++) {
            int idx = (startIndex + step) % n;
            boolean dup = itemBuckets.get(idx).stream()
                    .anyMatch(it -> normalize(it.name()).equals(norm));
            if (!dup) {
                return idx;
            }
        }
        return startIndex; // 전부 겹치면(드묾) 원위치
    }

    /** 공백 제거 + 소문자화로 표기 차이를 흡수한 비교용 키. "돈가스 덮밥" == "돈가스덮밥". */
    private String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }

    /** 메뉴명으로 카테고리를 결정한다. (풀 → 키워드 추론 → MAIN_DISH) */
    private MenuCategory resolveCategory(String name, Map<String, MenuCategory> knownCategories) {
        MenuCategory known = knownCategories == null ? null : knownCategories.get(name);
        if (known != null) {
            return known;
        }
        return inferByKeyword(name);
    }

    /** 이름 키워드 기반 카테고리 추론. 모르면 MAIN_DISH. */
    private MenuCategory inferByKeyword(String name) {
        // 김치류 (조리어 없는 경우)
        boolean cooked = name.contains("밥") || name.contains("찌개") || name.contains("볶음")
                || name.contains("국") || name.contains("탕") || name.contains("전");
        if (!cooked && (name.endsWith("김치") || name.contains("깍두기") || name.contains("석박지")
                || name.contains("알타리") || name.contains("동치미"))) {
            return MenuCategory.KIMCHI;
        }
        // 음료
        if (name.contains("우유") || name.contains("주스") || name.contains("음료")
                || name.contains("차") || name.contains("에이드") || name.contains("라떼")) {
            return MenuCategory.BEVERAGE;
        }
        // 국/탕
        if (name.contains("국") || name.contains("탕") || name.contains("찌개") || name.endsWith("스프")) {
            return MenuCategory.SOUP;
        }
        // 주식 (밥·면·빵·죽 류). 덮밥/볶음밥/국수/파스타 등
        if (name.contains("밥") || name.contains("덮밥") || name.contains("국수") || name.contains("면")
                || name.contains("파스타") || name.contains("죽") || name.contains("빵")
                || name.contains("리조또") || name.contains("리소토")) {
            return MenuCategory.STAPLE_FOOD;
        }
        // 후식
        if (name.contains("과일") || name.contains("요거트") || name.contains("요구르트")
                || name.endsWith("떡") || name.contains("케이크")) {
            return MenuCategory.DESSERT;
        }
        // 기본: 주찬
        return MenuCategory.MAIN_DISH;
    }
}
