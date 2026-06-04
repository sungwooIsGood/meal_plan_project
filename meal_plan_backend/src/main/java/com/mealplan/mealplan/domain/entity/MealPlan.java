package com.mealplan.mealplan.domain.entity;

import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 식단표 Aggregate Root.
 *
 * <p>하나의 업로드 = 하나의 MealPlan. 분류된 메뉴 항목들을 카테고리별로 보관한다.
 *
 * <p>불변식: 분류된 메뉴 항목이 최소 1개 이상 존재해야 한다.
 * 0개라면 급식 식단표가 아니라고 판단하여 {@link NotMealPlanContentException} 을 던진다.
 */
public final class MealPlan {

    private final MealPlanId id;
    private final String sourceFileName;
    private final Instant importedAt;
    private final List<MenuItem> items;

    private MealPlan(MealPlanId id, String sourceFileName, Instant importedAt, List<MenuItem> items) {
        if (id == null) {
            throw new IllegalArgumentException("MealPlanId는 필수입니다.");
        }
        if (sourceFileName == null || sourceFileName.isBlank()) {
            throw new IllegalArgumentException("원본 파일명은 필수입니다.");
        }
        if (importedAt == null) {
            throw new IllegalArgumentException("import 시각은 필수입니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new NotMealPlanContentException(
                    "분류 가능한 급식 메뉴가 없습니다. 급식 식단표 엑셀 파일이 아닙니다.");
        }
        this.id = id;
        this.sourceFileName = sourceFileName.trim();
        this.importedAt = importedAt;
        this.items = List.copyOf(items);
    }

    /**
     * 분류된 메뉴 항목들로 새 식단표를 생성한다.
     *
     * @throws NotMealPlanContentException 분류된 메뉴가 0개일 때
     */
    public static MealPlan create(String sourceFileName, List<MenuItem> items) {
        return new MealPlan(MealPlanId.newId(), sourceFileName, Instant.now(), items);
    }

    /**
     * 영속 계층에서 기존 식단표를 복원할 때 사용.
     */
    public static MealPlan restore(MealPlanId id, String sourceFileName, Instant importedAt,
                                   List<MenuItem> items) {
        return new MealPlan(id, sourceFileName, importedAt, items);
    }

    public MealPlanId id() {
        return id;
    }

    public String sourceFileName() {
        return sourceFileName;
    }

    public Instant importedAt() {
        return importedAt;
    }

    public List<MenuItem> items() {
        return Collections.unmodifiableList(items);
    }

    public int itemCount() {
        return items.size();
    }

    /**
     * 카테고리별로 그룹핑된 메뉴 이름 목록을 반환한다.
     * 항목이 없는 카테고리는 결과에 포함되지 않는다.
     */
    public Map<MenuCategory, List<String>> itemsByCategory() {
        Map<MenuCategory, List<String>> grouped = new EnumMap<>(MenuCategory.class);
        for (MenuItem item : items) {
            grouped.computeIfAbsent(item.category(), k -> new ArrayList<>()).add(item.name());
        }
        return grouped;
    }
}
