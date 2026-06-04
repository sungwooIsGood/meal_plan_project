package com.mealplan.mealplan.secondary.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.secondary.persistence.adapter.MealPlanPersistenceAdapter;
import com.mealplan.mealplan.secondary.persistence.document.MealPlanMongoRepository;
import com.mealplan.mealplan.support.MongoTestContainerConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

/**
 * MongoDB 영속성 어댑터 통합 테스트.
 *
 * <p>Testcontainers 로 실제 mongod(mongo:7.0) 컨테이너를 띄워 검증한다. 도커 데몬이 실행 중이어야 한다.
 */
@DataMongoTest
@Import({MealPlanPersistenceAdapter.class, MongoTestContainerConfig.class})
class MealPlanPersistenceAdapterIT {

    @Autowired
    private MealPlanPersistenceAdapter adapter;

    @Autowired
    private MealPlanMongoRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void saves_and_finds_meal_plan_round_trip() {
        MealPlan plan = MealPlan.create("2026-06.xlsx", List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("미역국", MenuCategory.SOUP),
                MenuItem.of("배추김치", MenuCategory.KIMCHI)));

        MealPlan saved = adapter.save(plan);

        Optional<MealPlan> found = adapter.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().sourceFileName()).isEqualTo("2026-06.xlsx");
        assertThat(found.get().itemCount()).isEqualTo(3);
        assertThat(found.get().itemsByCategory().get(MenuCategory.KIMCHI))
                .containsExactly("배추김치");
    }

    @Test
    void findAll_returns_saved_meal_plans() {
        adapter.save(MealPlan.create("a.xlsx",
                List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD))));
        adapter.save(MealPlan.create("b.xlsx",
                List.of(MenuItem.of("미역국", MenuCategory.SOUP))));

        List<MealPlan> all = adapter.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(MealPlan::sourceFileName)
                .containsExactlyInAnyOrder("a.xlsx", "b.xlsx");
    }

    @Test
    void deleteByIds_removes_only_selected_meal_plans() {
        MealPlan a = adapter.save(MealPlan.create("a.xlsx",
                List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD))));
        MealPlan b = adapter.save(MealPlan.create("b.xlsx",
                List.of(MenuItem.of("미역국", MenuCategory.SOUP))));
        adapter.save(MealPlan.create("c.xlsx",
                List.of(MenuItem.of("배추김치", MenuCategory.KIMCHI))));

        long deleted = adapter.deleteByIds(List.of(a.id(), b.id()));

        assertThat(deleted).isEqualTo(2);
        assertThat(adapter.findAll()).extracting(MealPlan::sourceFileName)
                .containsExactly("c.xlsx");
    }

    @Test
    void deleteByIds_empty_list_deletes_nothing() {
        adapter.save(MealPlan.create("a.xlsx",
                List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD))));

        long deleted = adapter.deleteByIds(List.of());

        assertThat(deleted).isZero();
        assertThat(adapter.findAll()).hasSize(1);
    }

    @Test
    void loadCandidatePool_aggregates_unique_menus_by_category_across_documents() {
        // 두 문서에 걸쳐 같은 메뉴가 반복 → 후보 풀에서는 카테고리별 고유 메뉴로 집계되어야 함
        adapter.save(MealPlan.create("a.xlsx", List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("미역국", MenuCategory.SOUP),
                MenuItem.of("배추김치", MenuCategory.KIMCHI))));
        adapter.save(MealPlan.create("b.xlsx", List.of(
                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),   // 중복 등장(빈도 2)
                MenuItem.of("흑미밥", MenuCategory.STAPLE_FOOD),
                MenuItem.of("된장찌개", MenuCategory.SOUP))));

        var pool = adapter.loadCandidatePool();

        // 주식: 흰쌀밥, 흑미밥 (고유 2종)
        assertThat(pool.get(MenuCategory.STAPLE_FOOD))
                .extracting(MenuItem::name)
                .containsExactlyInAnyOrder("흰쌀밥", "흑미밥");
        // 국/탕: 미역국, 된장찌개
        assertThat(pool.get(MenuCategory.SOUP))
                .extracting(MenuItem::name)
                .containsExactlyInAnyOrder("미역국", "된장찌개");
        // 김치: 배추김치
        assertThat(pool.get(MenuCategory.KIMCHI))
                .extracting(MenuItem::name)
                .containsExactly("배추김치");
    }

    @Test
    void loadCandidatePool_caps_count_per_category_by_policy() {
        // 부찬을 정책 개수(120)보다 많게 저장 → 후보 풀은 정책 상한으로 잘려야 함
        java.util.List<MenuItem> manySides = new java.util.ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            manySides.add(MenuItem.of("부찬_" + i, MenuCategory.SIDE_DISH));
        }
        adapter.save(MealPlan.create("big.xlsx", manySides));

        var pool = adapter.loadCandidatePool();

        int cap = com.mealplan.mealplan.domain.entity.GenerationRules
                .CANDIDATE_POOL_SIZE.get(MenuCategory.SIDE_DISH);
        assertThat(pool.get(MenuCategory.SIDE_DISH)).hasSize(cap); // 200 → 120으로 캡
    }
}
