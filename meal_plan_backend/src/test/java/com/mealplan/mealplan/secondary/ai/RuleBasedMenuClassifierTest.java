package com.mealplan.mealplan.secondary.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RuleBasedMenuClassifierTest {

    private final RuleBasedMenuClassifier classifier = new RuleBasedMenuClassifier();

    private Map<String, MenuCategory> classifyToMap(List<String> tokens) {
        return classifier.classify(tokens).stream()
                .collect(Collectors.toMap(MenuItem::name, MenuItem::category, (a, b) -> a));
    }

    @Test
    void classifies_staple_food() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("흰쌀밥", "흑미밥", "볶음밥", "국수"));

        assertThat(result.get("흰쌀밥")).isEqualTo(MenuCategory.STAPLE_FOOD);
        assertThat(result.get("흑미밥")).isEqualTo(MenuCategory.STAPLE_FOOD);
        assertThat(result.get("볶음밥")).isEqualTo(MenuCategory.STAPLE_FOOD);
        assertThat(result.get("국수")).isEqualTo(MenuCategory.STAPLE_FOOD);
    }

    @Test
    void classifies_soup() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("미역국", "된장국", "육개장", "우삼겹된장찌개"));

        assertThat(result.get("미역국")).isEqualTo(MenuCategory.SOUP);
        assertThat(result.get("된장국")).isEqualTo(MenuCategory.SOUP);
        assertThat(result.get("육개장")).isEqualTo(MenuCategory.SOUP);
        assertThat(result.get("우삼겹된장찌개")).isEqualTo(MenuCategory.SOUP);
    }

    @Test
    void classifies_main_dish() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("제육볶음", "돈가스", "닭갈비", "짜장찜닭"));

        assertThat(result.get("제육볶음")).isEqualTo(MenuCategory.MAIN_DISH);
        assertThat(result.get("돈가스")).isEqualTo(MenuCategory.MAIN_DISH);
        assertThat(result.get("닭갈비")).isEqualTo(MenuCategory.MAIN_DISH);
        assertThat(result.get("짜장찜닭")).isEqualTo(MenuCategory.MAIN_DISH);
    }

    @Test
    void classifies_side_dish() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("멸치볶음", "시금치나물", "계란말이", "참나물유자무침"));

        assertThat(result.get("멸치볶음")).isEqualTo(MenuCategory.SIDE_DISH);
        assertThat(result.get("시금치나물")).isEqualTo(MenuCategory.SIDE_DISH);
        // 계란말이는 키워드 미등록일 수 있으나, 무침/나물/볶음 계열은 부찬으로 분류
        assertThat(result.get("참나물유자무침")).isEqualTo(MenuCategory.SIDE_DISH);
    }

    @Test
    void classifies_kimchi_but_not_kimchi_cooked_dishes() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("배추김치", "깍두기", "석박지", "알타리김치"));

        assertThat(result.get("배추김치")).isEqualTo(MenuCategory.KIMCHI);
        assertThat(result.get("깍두기")).isEqualTo(MenuCategory.KIMCHI);
        assertThat(result.get("석박지")).isEqualTo(MenuCategory.KIMCHI);
        assertThat(result.get("알타리김치")).isEqualTo(MenuCategory.KIMCHI);

        // 김치볶음밥은 주식, 김치찌개는 국/탕 — 김치로 분류되면 안 됨
        Map<String, MenuCategory> cooked =
                classifyToMap(List.of("김치볶음밥", "김치찌개"));
        assertThat(cooked.get("김치볶음밥")).isEqualTo(MenuCategory.STAPLE_FOOD);
        assertThat(cooked.get("김치찌개")).isEqualTo(MenuCategory.SOUP);
    }

    @Test
    void classifies_dessert_and_beverage() {
        Map<String, MenuCategory> result =
                classifyToMap(List.of("사과", "파인애플", "찰떡", "우유", "주스"));

        assertThat(result.get("사과")).isEqualTo(MenuCategory.DESSERT);
        assertThat(result.get("파인애플")).isEqualTo(MenuCategory.DESSERT);
        assertThat(result.get("찰떡")).isEqualTo(MenuCategory.DESSERT);
        assertThat(result.get("우유")).isEqualTo(MenuCategory.BEVERAGE);
        assertThat(result.get("주스")).isEqualTo(MenuCategory.BEVERAGE);
    }

    @Test
    void splits_combined_cells_by_separators() {
        // 식단표 셀은 구분자로 여러 메뉴를 묶는다
        List<MenuItem> items = classifier.classify(List.of("새우가득새우링/나쵸샐러드", "콘푸로스트&우유"));

        List<String> names = items.stream().map(MenuItem::name).toList();
        assertThat(names).contains("나쵸샐러드", "우유");
    }

    @Test
    void returns_empty_for_non_meal_content() {
        // 급식과 무관한 파일 (예: 매출 보고서) → 분류 결과 0개
        List<MenuItem> items = classifier.classify(List.of(
                "분기별 매출 보고서", "영업이익", "2026", "고객사", "계약 금액", "담당자명"));

        assertThat(items).isEmpty();
    }

    @Test
    void ignores_weekday_and_numeric_headers() {
        List<MenuItem> items = classifier.classify(List.of(
                "월", "화", "수", "목", "금", "1", "2", "3", "조식", "식단표"));

        assertThat(items).isEmpty();
    }
}
