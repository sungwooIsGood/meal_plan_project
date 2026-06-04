package com.mealplan.mealplan.application.output;

import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MealPlan Aggregate 영속성 Output Port.
 *
 * <p>구현체: {@code secondary/persistence/adapter/MealPlanPersistenceAdapter}.
 */
public interface MealPlanRepositoryPort {

    MealPlan save(MealPlan mealPlan);

    Optional<MealPlan> findById(MealPlanId id);

    /** 저장된 모든 식단표를 import 최신순으로 반환한다. */
    List<MealPlan> findAll();

    /**
     * 주어진 ID들의 식단표를 삭제한다.
     *
     * @param ids 삭제할 식단표 ID 목록
     * @return 실제로 삭제된 개수
     */
    long deleteByIds(List<MealPlanId> ids);

    /**
     * 식단 생성용 후보 메뉴 풀을 카테고리별로 반환한다.
     *
     * <p>전체 메뉴를 다 주지 않고, DB 집계로 카테고리별 "빈도 상위 + 랜덤 샘플"만 추려서 반환한다.
     * 전국 규모로 데이터가 커져도 호출자가 AI에 보내는 후보 수(=토큰)를 일정하게 유지하기 위함이다.
     *
     * <p>카테고리별 개수와 인기:랜덤 비율은 {@code GenerationRules}의 정책을 따른다.
     * 어떤 카테고리의 고유 메뉴가 정책 개수보다 적으면 있는 것 전부 반환한다.
     *
     * @return 카테고리 → 후보 메뉴 목록
     */
    Map<MenuCategory, List<MenuItem>> loadCandidatePool();
}
