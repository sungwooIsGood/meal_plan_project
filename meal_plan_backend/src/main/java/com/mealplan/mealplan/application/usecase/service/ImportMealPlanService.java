package com.mealplan.mealplan.application.usecase.service;

import com.mealplan.mealplan.application.output.MealPlanFileReaderPort;
import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.application.output.MenuClassifierPort;
import com.mealplan.mealplan.application.usecase.ImportMealPlanCommand;
import com.mealplan.mealplan.application.usecase.ImportMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MenuItem;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 식단표 import UseCase 구현. 비즈니스 흐름을 조율한다.
 *
 * <p>오케스트레이션: 파일에서 토큰 추출 → AI 분류 → 도메인 생성(불변식 검증) → 저장.
 * 모든 외부 의존은 {@code application/output}의 포트로만 접근한다(어댑터 직접 참조 금지).
 */
@Service
public class ImportMealPlanService implements ImportMealPlanUseCase {

    private final MealPlanFileReaderPort fileReader;
    private final MenuClassifierPort classifier;
    private final MealPlanRepositoryPort repository;

    public ImportMealPlanService(MealPlanFileReaderPort fileReader,
                                 MenuClassifierPort classifier,
                                 MealPlanRepositoryPort repository) {
        this.fileReader = fileReader;
        this.classifier = classifier;
        this.repository = repository;
    }

    @Override
    public MealPlan importMealPlan(ImportMealPlanCommand command) {
        // 1) 엑셀에서 텍스트 토큰 추출 (xlsx 아님/깨짐 → InvalidExcelFileException)
        List<String> tokens = fileReader.extractTokens(command.originalFileName(), command.content());

        // 2) AI/규칙 기반 분류
        List<MenuItem> items = classifier.classify(tokens);

        // 3) 도메인 생성 — 분류 결과 0개면 NotMealPlanContentException (급식 아님)
        MealPlan mealPlan = MealPlan.create(command.originalFileName(), items);

        // 4) 저장
        return repository.save(mealPlan);
    }
}
