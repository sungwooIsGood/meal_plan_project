package com.mealplan.mealplan.application.usecase.service;

import com.mealplan.mealplan.application.output.MealPlanGeneratorPort;
import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.usecase.GenerateMealPlanCommand;
import com.mealplan.mealplan.application.usecase.GenerateMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.GenerationRules;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import com.mealplan.mealplan.domain.service.DeterministicMealPlanGenerator;
import com.mealplan.mealplan.domain.service.MealPlanRuleValidator;
import com.mealplan.mealplan.domain.service.MustIncludeEnforcer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 식단 생성 UseCase 구현.
 *
 * <p>흐름: 저장된 메뉴 풀 수집 → AI 생성 시도 → 도메인 검증 → 위반 시 결정론적 생성기로 fallback
 * → "꼭 포함할 메뉴" 강제 포함(DB에 없어도).
 * 규칙 준수는 결정론적 검증기/생성기가 최종 보장한다(LLM 결과를 맹신하지 않음).
 */
@Service
public class GenerateMealPlanService implements GenerateMealPlanUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateMealPlanService.class);

    private final MealPlanRepositoryPort repository;
    private final MealPlanGeneratorPort aiGenerator;
    private final MealPlanRuleValidator validator = new MealPlanRuleValidator();
    private final DeterministicMealPlanGenerator deterministicGenerator =
            new DeterministicMealPlanGenerator();
    private final MustIncludeEnforcer mustIncludeEnforcer = new MustIncludeEnforcer();

    public GenerateMealPlanService(MealPlanRepositoryPort repository,
                                   MealPlanGeneratorPort aiGenerator) {
        this.repository = repository;
        this.aiGenerator = aiGenerator;
    }

    @Override
    public Result generate(GenerateMealPlanCommand command) {
        Map<MenuCategory, List<MenuItem>> pool = repository.loadCandidatePool();
        if (pool.isEmpty()) {
            throw new NotMealPlanContentException(
                    "저장된 식단 데이터가 없습니다. 먼저 엑셀을 업로드하세요.");
        }

        String ruleSummary = buildRuleSummary();
        int sideDishCount = command.hasRequirements() ? 1 : 1; // 기본 1 (요구사항 파싱은 AI가 반영)
        Map<String, MenuCategory> knownCategories = buildNameToCategory(pool);

        // 1) AI 생성 시도
        List<PlannedMeal> aiMeals = List.of();
        try {
            aiMeals = aiGenerator.generate(
                    command.targetDates(),
                    command.mealTypes(),
                    pool,
                    command.requirements(),
                    command.mustIncludeMenus(),
                    ruleSummary);
        } catch (Exception e) {
            log.warn("AI 식단 생성 실패, 결정론적 생성기로 fallback: {}", e.getMessage());
        }

        // 2) AI 결과 검증 (꼭 포함할 메뉴 강제 적용 후)
        if (!aiMeals.isEmpty()) {
            List<PlannedMeal> enforced = mustIncludeEnforcer.enforce(
                    aiMeals, command.mustIncludeMenus(), knownCategories);
            List<String> violations = validator.validate(enforced);
            if (violations.isEmpty()) {
                return new Result(enforced, List.of(), true);
            }
            // 요구사항이 있으면 요구사항이 기본 규칙보다 우선이다(사용자가 직접 입력).
            // 결정론적 생성기는 자유 텍스트 요구사항을 반영하지 못하므로 AI 결과를 그대로 유지한다.
            // 이때의 중복 규칙 위반은 "사용자가 의도적으로 규칙을 덮어쓴 결과"이므로 경고로 띄우지 않는다.
            if (command.hasRequirements()) {
                log.info("요구사항 우선: AI 결과 유지(규칙 위반 {}건은 요구사항 우선으로 허용)", violations.size());
                return new Result(enforced, List.of(), true);
            }
            log.info("AI 생성 결과가 규칙 {}건 위반 → 결정론적 생성기로 대체", violations.size());
        }

        // 3) 결정론적 생성 (규칙 보장)
        List<PlannedMeal> meals = deterministicGenerator.generate(
                command.targetDates(), command.mealTypes(), pool, sideDishCount);
        // 꼭 포함할 메뉴 강제 적용 (DB에 없어도 포함)
        meals = mustIncludeEnforcer.enforce(meals, command.mustIncludeMenus(), knownCategories);
        List<String> violations = validator.validate(meals);
        return new Result(meals, violations, false);
    }

    /** 후보 풀에서 (메뉴명 → 카테고리) 맵을 만든다. */
    private Map<String, MenuCategory> buildNameToCategory(Map<MenuCategory, List<MenuItem>> pool) {
        Map<String, MenuCategory> map = new HashMap<>();
        pool.forEach((cat, items) -> items.forEach(i -> map.putIfAbsent(i.name(), cat)));
        return map;
    }

    private String buildRuleSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("하루 한 끼 구성: ").append(String.join(", ", GenerationRules.DAILY_COMPOSITION)).append("\n");
        sb.append("중복 금지(끼니 통합 전체 타임라인 기준): ");
        GenerationRules.NO_REPEAT_WITHIN_DAYS.forEach((cat, days) ->
                sb.append(cat.koreanName()).append("=").append(days).append("일 ").append(' '));
        sb.append("\n");
        sb.append("단, 주식 세부 규칙: 일반 곡물밥(흰쌀밥·현미밥·흑미밥·잡곡밥 등)은 7일 내 금지, ")
          .append("그 외 일품 주식(볶음밥·비빔밥·콩나물밥·덮밥·국수·파스타·빵 등)은 30일 내 금지.\n");
        GenerationRules.CROSS_MEAL_RULES.forEach(r -> sb.append("- ").append(r).append("\n"));
        return sb.toString();
    }
}
