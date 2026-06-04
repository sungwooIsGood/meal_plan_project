package com.mealplan.mealplan.primary.rest.dto;

import java.time.Instant;

/**
 * 표준 에러 응답 DTO.
 *
 * @param status  HTTP 상태 코드
 * @param error   에러 코드(짧은 식별자)
 * @param message 사람이 읽을 수 있는 메시지
 * @param timestamp 발생 시각
 */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, Instant.now());
    }
}
