package com.mealplan.mealplan.primary.rest;

import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import com.mealplan.mealplan.domain.exception.MealPlanNotFoundException;
import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import com.mealplan.mealplan.primary.rest.dto.ErrorResponse;
import java.time.DateTimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

/**
 * 웹 어댑터 전역 예외 처리.
 *
 * <ul>
 *   <li>{@link InvalidExcelFileException} → 400 Bad Request (엑셀 아님/깨짐/누락)</li>
 *   <li>{@link NotMealPlanContentException} → 422 Unprocessable Entity (급식 내용 아님)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidExcelFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExcel(InvalidExcelFileException e) {
        log.info("잘못된 엑셀 파일 업로드: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "INVALID_EXCEL_FILE", e.getMessage()));
    }

    @ExceptionHandler(NotMealPlanContentException.class)
    public ResponseEntity<ErrorResponse> handleNotMealPlanContent(NotMealPlanContentException e) {
        log.info("급식 내용이 아닌 파일 업로드: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "NOT_MEAL_PLAN_CONTENT", e.getMessage()));
    }

    @ExceptionHandler(MealPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(MealPlanNotFoundException e) {
        log.info("식단표 조회 실패: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                        "MEAL_PLAN_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handleMissingFile(Exception e) {
        log.info("멀티파트 요청 오류: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REQUEST", "엑셀 파일(form field 'file')을 multipart/form-data로 업로드해야 합니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.info("잘못된 요청 값: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ErrorResponse> handleDateTime(DateTimeException e) {
        log.info("잘못된 년월: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "INVALID_YEAR_MONTH", "년월이 올바르지 않습니다. 월은 1~12 사이여야 합니다."));
    }
}
