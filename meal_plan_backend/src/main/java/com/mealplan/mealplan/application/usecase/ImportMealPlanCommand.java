package com.mealplan.mealplan.application.usecase;

import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;

/**
 * 식단표 import 요청 커맨드.
 *
 * @param originalFileName 업로드된 원본 파일명
 * @param contentType      MIME 타입 (nullable)
 * @param content          파일 바이트
 */
public record ImportMealPlanCommand(String originalFileName, String contentType, byte[] content) {

    public ImportMealPlanCommand {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidExcelFileException("파일명이 없습니다.");
        }
        if (content == null || content.length == 0) {
            throw new InvalidExcelFileException("빈 파일입니다.");
        }
    }
}
