package com.mealplan.mealplan.domain.exception;

/**
 * 업로드된 파일이 읽을 수 없거나 .xlsx 형식이 아닐 때 발생.
 *
 * <p>웹 어댑터에서 400(Bad Request)으로 매핑된다.
 */
public class InvalidExcelFileException extends RuntimeException {

    public InvalidExcelFileException(String message) {
        super(message);
    }

    public InvalidExcelFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
