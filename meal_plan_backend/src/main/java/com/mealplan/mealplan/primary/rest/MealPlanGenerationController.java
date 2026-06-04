package com.mealplan.mealplan.primary.rest;

import com.mealplan.mealplan.application.usecase.GenerateMealPlanUseCase;
import com.mealplan.mealplan.primary.rest.dto.GenerateMealPlanRequest;
import com.mealplan.mealplan.primary.rest.dto.GenerateMealPlanResponse;
import com.mealplan.mealplan.primary.rest.dto.GenerationRulesResponse;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 식단 생성 Web 어댑터 (Inbound).
 */
@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanGenerationController {

    private final GenerateMealPlanUseCase generateMealPlanUseCase;

    public MealPlanGenerationController(GenerateMealPlanUseCase generateMealPlanUseCase) {
        this.generateMealPlanUseCase = generateMealPlanUseCase;
    }

    /**
     * 저장된 메뉴 풀을 바탕으로 해당 년월 식단을 생성한다.
     *
     * @param year    년 (예: 2026)
     * @param month   월 (1~12)
     * @param request 생성 조건 (선택). 없으면 기본 규칙 적용.
     */
    @PostMapping(path = "/generate/{year}/{month}")
    public ResponseEntity<GenerateMealPlanResponse> generate(
            @PathVariable int year,
            @PathVariable int month,
            @RequestBody(required = false) GenerateMealPlanRequest request) {

        YearMonth yearMonth = YearMonth.of(year, month);
        GenerateMealPlanRequest req = request != null
                ? request
                : new GenerateMealPlanRequest(null, null, null, null, null, null);

        GenerateMealPlanUseCase.Result result =
                generateMealPlanUseCase.generate(req.toCommand(yearMonth));

        return ResponseEntity.ok(GenerateMealPlanResponse.from(result));
    }

    /**
     * 기본 생성 규칙 안내 (프론트엔드 표시용).
     */
    @GetMapping("/generation-rules")
    public ResponseEntity<GenerationRulesResponse> generationRules() {
        return ResponseEntity.ok(GenerationRulesResponse.current());
    }
}
