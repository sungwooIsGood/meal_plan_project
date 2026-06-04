package com.mealplan.mealplan.primary.rest;

import com.mealplan.mealplan.application.usecase.DeleteMealPlanUseCase;
import com.mealplan.mealplan.application.usecase.GetMealPlanUseCase;
import com.mealplan.mealplan.application.usecase.ImportMealPlanCommand;
import com.mealplan.mealplan.application.usecase.ImportMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import com.mealplan.mealplan.primary.rest.dto.DeleteMealPlansRequest;
import com.mealplan.mealplan.primary.rest.dto.DeleteMealPlansResponse;
import com.mealplan.mealplan.primary.rest.dto.ImportMealPlanResponse;
import com.mealplan.mealplan.primary.rest.dto.MealPlanSummaryResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 식단표 Web 어댑터 (Inbound). 업로드(import) + 조회 + 삭제.
 */
@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    private final ImportMealPlanUseCase importMealPlanUseCase;
    private final GetMealPlanUseCase getMealPlanUseCase;
    private final DeleteMealPlanUseCase deleteMealPlanUseCase;

    public MealPlanController(ImportMealPlanUseCase importMealPlanUseCase,
                              GetMealPlanUseCase getMealPlanUseCase,
                              DeleteMealPlanUseCase deleteMealPlanUseCase) {
        this.importMealPlanUseCase = importMealPlanUseCase;
        this.getMealPlanUseCase = getMealPlanUseCase;
        this.deleteMealPlanUseCase = deleteMealPlanUseCase;
    }

    /**
     * 엑셀(.xlsx) 식단표 파일을 업로드하여 분류·저장한다.
     *
     * @param file 멀티파트 엑셀 파일 (form field name: "file")
     * @return 201 Created + 분류 결과
     */
    @PostMapping(path = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ImportMealPlanResponse> importMealPlan(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidExcelFileException("업로드된 파일이 없습니다.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidExcelFileException("파일을 읽는 중 오류가 발생했습니다.", e);
        }

        ImportMealPlanCommand command = new ImportMealPlanCommand(
                file.getOriginalFilename(),
                file.getContentType(),
                content);

        MealPlan mealPlan = importMealPlanUseCase.importMealPlan(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ImportMealPlanResponse.from(mealPlan));
    }

    /**
     * 저장된 식단표 단건을 ID로 조회한다. (방금 업로드한 결과 확인용)
     *
     * @param id import 시 반환된 식단표 ID
     * @return 200 OK + 카테고리별 분류 결과 (없으면 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImportMealPlanResponse> getMealPlan(@PathVariable String id) {
        MealPlan mealPlan = getMealPlanUseCase.getById(id);
        return ResponseEntity.ok(ImportMealPlanResponse.from(mealPlan));
    }

    /**
     * 저장된 식단표 목록을 import 최신순으로 조회한다. (요약)
     *
     * @return 200 OK + 요약 목록
     */
    @GetMapping
    public ResponseEntity<List<MealPlanSummaryResponse>> listMealPlans() {
        List<MealPlanSummaryResponse> summaries = getMealPlanUseCase.getAll().stream()
                .map(MealPlanSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    /**
     * 선택한 식단표들을 일괄 삭제한다.
     *
     * @param request 삭제할 식단표 ID 목록 {@code {"ids": [...]}}
     * @return 200 OK + 삭제된 개수
     */
    @DeleteMapping
    public ResponseEntity<DeleteMealPlansResponse> deleteMealPlans(
            @RequestBody DeleteMealPlansRequest request) {
        long deleted = deleteMealPlanUseCase.deleteByIds(
                request != null ? request.ids() : List.of());
        return ResponseEntity.ok(new DeleteMealPlansResponse(deleted));
    }
}
