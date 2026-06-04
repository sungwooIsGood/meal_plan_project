package com.mealplan.mealplan.secondary.persistence.mapper;

import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.StapleType;
import com.mealplan.mealplan.secondary.persistence.document.MealPlanDocument;
import java.util.List;

/**
 * 도메인 모델 <-> MongoDB 문서 매퍼.
 */
public final class MealPlanMapper {

    private MealPlanMapper() {
    }

    public static MealPlanDocument toDocument(MealPlan mealPlan) {
        List<MealPlanDocument.MenuItemDocument> itemDocs = mealPlan.items().stream()
                .map(item -> new MealPlanDocument.MenuItemDocument(
                        item.name(),
                        item.category().name(),
                        item.stapleType() != null ? item.stapleType().name() : null))
                .toList();
        return new MealPlanDocument(
                mealPlan.id().value(),
                mealPlan.sourceFileName(),
                mealPlan.importedAt(),
                itemDocs);
    }

    public static MealPlan toDomain(MealPlanDocument document) {
        List<MenuItem> items = document.getItems().stream()
                .map(MealPlanMapper::toMenuItem)
                .toList();
        return MealPlan.restore(
                MealPlanId.of(document.getId()),
                document.getSourceFileName(),
                document.getImportedAt(),
                items);
    }

    private static MenuItem toMenuItem(MealPlanDocument.MenuItemDocument doc) {
        MenuCategory category = MenuCategory.valueOf(doc.getCategory());
        StapleType stapleType = parseStapleType(doc.getStapleType());
        // stapleType이 null이면 MenuItem이 이름 키워드로 추론(구버전 데이터 호환).
        return MenuItem.of(doc.getName(), category, stapleType);
    }

    private static StapleType parseStapleType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StapleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
