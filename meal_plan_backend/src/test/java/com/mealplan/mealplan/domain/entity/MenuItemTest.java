package com.mealplan.mealplan.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MenuItemTest {

    @Test
    void of_creates_item_and_trims_name() {
        MenuItem item = MenuItem.of("  제육볶음 ", MenuCategory.MAIN_DISH);

        assertThat(item.name()).isEqualTo("제육볶음");
        assertThat(item.category()).isEqualTo(MenuCategory.MAIN_DISH);
    }

    @Test
    void should_throw_when_name_is_blank() {
        assertThatThrownBy(() -> MenuItem.of("  ", MenuCategory.SOUP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("메뉴 이름");
    }

    @Test
    void should_throw_when_name_is_null() {
        assertThatThrownBy(() -> MenuItem.of(null, MenuCategory.SOUP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_when_category_is_null() {
        assertThatThrownBy(() -> MenuItem.of("된장국", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("분류");
    }

    @Test
    void items_with_same_name_and_category_are_equal() {
        MenuItem a = MenuItem.of("배추김치", MenuCategory.KIMCHI);
        MenuItem b = MenuItem.of("배추김치", MenuCategory.KIMCHI);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
