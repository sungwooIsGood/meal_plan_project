package com.mealplan.mealplan.domain.entity;

import java.util.Objects;

/**
 * 분류된 단일 메뉴 항목 (Value Object). 불변.
 *
 * <p>불변식: 메뉴 이름은 공백이 아니어야 하고, 분류는 반드시 존재해야 한다.
 *
 * <p>주식(STAPLE_FOOD)인 경우 {@link StapleType}(일반밥 RICE / 일품 DISH)을 함께 가진다.
 * 이는 중복 금지 규칙 차등 적용에만 쓰이며 화면에는 노출하지 않는다.
 * 주식이 아니면 {@code stapleType}은 null.
 */
public final class MenuItem {

    private final String name;
    private final MenuCategory category;
    private final StapleType stapleType;

    private MenuItem(String name, MenuCategory category, StapleType stapleType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("메뉴 이름은 비어 있을 수 없습니다.");
        }
        if (category == null) {
            throw new IllegalArgumentException("메뉴 분류는 필수입니다.");
        }
        this.name = name.trim();
        this.category = category;
        // 주식이면 stapleType 보장(미지정 시 키워드로 추론), 주식 아니면 null로 정규화.
        if (category == MenuCategory.STAPLE_FOOD) {
            this.stapleType = stapleType != null ? stapleType : inferStapleType(this.name);
        } else {
            this.stapleType = null;
        }
    }

    /** 카테고리만으로 생성 (주식이면 stapleType은 이름 키워드로 추론). */
    public static MenuItem of(String name, MenuCategory category) {
        return new MenuItem(name, category, null);
    }

    /** 카테고리 + 주식 세부유형 명시 생성. */
    public static MenuItem of(String name, MenuCategory category, StapleType stapleType) {
        return new MenuItem(name, category, stapleType);
    }

    public String name() {
        return name;
    }

    public MenuCategory category() {
        return category;
    }

    /** 주식이면 RICE/DISH, 아니면 null. */
    public StapleType stapleType() {
        return stapleType;
    }

    /**
     * 일반 곡물밥 여부. 주식이면서 stapleType이 RICE일 때만 true.
     */
    public boolean isSimpleRice() {
        return category == MenuCategory.STAPLE_FOOD && stapleType == StapleType.RICE;
    }

    /**
     * stapleType 미지정 시 이름 키워드로 추론하는 fallback.
     * (AI/명시값이 없을 때만 사용 — 일품 키워드가 있으면 DISH, 그 외 '밥'으로 끝나면 RICE, 나머지 DISH)
     */
    private static StapleType inferStapleType(String name) {
        String n = name;
        // 일품 키워드: 한 끼 자체가 되는 주식
        String[] dishKeywords = {
                "볶음밥", "비빔밥", "덮밥", "국수", "면", "파스타", "리조또", "리소토", "죽",
                "빵", "버거", "타코", "카레", "리필라프", "필라프", "초밥", "주먹밥", "김밥",
                "샌드위치", "또띠아", "부리또", "누룽지", "떡국", "만둣국", "라면", "우동", "쌀국수"
        };
        for (String kw : dishKeywords) {
            if (n.contains(kw)) {
                return StapleType.DISH;
            }
        }
        // 단순 '밥'으로 끝나면 일반 곡물밥(예: 흰쌀밥, 현미밥, 단호박밥)
        if (n.endsWith("밥")) {
            return StapleType.RICE;
        }
        // 밥이 아닌 주식(면·빵 등 키워드 못 잡은 경우)은 일품으로 간주
        return StapleType.DISH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MenuItem other)) {
            return false;
        }
        return name.equals(other.name) && category == other.category
                && stapleType == other.stapleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category, stapleType);
    }

    @Override
    public String toString() {
        return "MenuItem{name='" + name + "', category=" + category
                + (stapleType != null ? ", stapleType=" + stapleType : "") + '}';
    }
}
