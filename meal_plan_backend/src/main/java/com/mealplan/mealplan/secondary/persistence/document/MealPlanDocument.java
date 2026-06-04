package com.mealplan.mealplan.secondary.persistence.document;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB 영속성 모델. 도메인 모델과 분리되어 있으며 매퍼로 변환한다.
 *
 * <p>하루치(=업로드 1건) 식단표를 하나의 문서로 저장한다. 카테고리별 메뉴는 가변 길이이므로
 * 문서 모델이 적합하다.
 */
@Document(collection = "meal_plans")
public class MealPlanDocument {

    @Id
    private String id;
    private String sourceFileName;
    private Instant importedAt;
    private List<MenuItemDocument> items;

    public MealPlanDocument() {
    }

    public MealPlanDocument(String id, String sourceFileName, Instant importedAt,
                            List<MenuItemDocument> items) {
        this.id = id;
        this.sourceFileName = sourceFileName;
        this.importedAt = importedAt;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }

    public List<MenuItemDocument> getItems() {
        return items;
    }

    public void setItems(List<MenuItemDocument> items) {
        this.items = items;
    }

    /** 내장 메뉴 항목 문서. */
    public static class MenuItemDocument {
        private String name;
        private String category;
        /** 주식 세부유형(RICE/DISH). 주식이 아니거나 구버전 데이터면 null. */
        private String stapleType;

        public MenuItemDocument() {
        }

        public MenuItemDocument(String name, String category, String stapleType) {
            this.name = name;
            this.category = category;
            this.stapleType = stapleType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStapleType() {
            return stapleType;
        }

        public void setStapleType(String stapleType) {
            this.stapleType = stapleType;
        }
    }
}
