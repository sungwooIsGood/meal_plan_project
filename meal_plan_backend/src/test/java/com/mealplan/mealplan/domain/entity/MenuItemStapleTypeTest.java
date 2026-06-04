package com.mealplan.mealplan.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MenuItemStapleTypeTest {

    @Test
    void non_staple_has_null_staple_type() {
        MenuItem item = MenuItem.of("미역국", MenuCategory.SOUP);
        assertThat(item.stapleType()).isNull();
        assertThat(item.isSimpleRice()).isFalse();
    }

    @Test
    void explicit_rice_is_simple_rice() {
        MenuItem item = MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD, StapleType.RICE);
        assertThat(item.isSimpleRice()).isTrue();
    }

    @Test
    void explicit_dish_is_not_simple_rice() {
        MenuItem item = MenuItem.of("콩나물밥", MenuCategory.STAPLE_FOOD, StapleType.DISH);
        assertThat(item.stapleType()).isEqualTo(StapleType.DISH);
        assertThat(item.isSimpleRice()).isFalse();
    }

    @Test
    void staple_without_type_infers_rice_for_plain_rice() {
        // 안전망: stapleType 미지정 시 이름으로 추론. '흰쌀밥'은 RICE.
        MenuItem item = MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD);
        assertThat(item.isSimpleRice()).isTrue();
    }

    @Test
    void staple_without_type_infers_dish_for_fried_rice() {
        // '볶음밥'은 일품 키워드 → DISH로 추론.
        MenuItem item = MenuItem.of("새우볶음밥", MenuCategory.STAPLE_FOOD);
        assertThat(item.isSimpleRice()).isFalse();
        assertThat(item.stapleType()).isEqualTo(StapleType.DISH);
    }

    @Test
    void staple_without_type_infers_dish_for_noodles() {
        MenuItem item = MenuItem.of("잔치국수", MenuCategory.STAPLE_FOOD);
        assertThat(item.stapleType()).isEqualTo(StapleType.DISH);
    }
}
