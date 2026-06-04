package com.mealplan.mealplan.secondary.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 엑셀에서 추출된 원시 셀 텍스트를 개별 메뉴 후보 문자열로 분해하는 유틸.
 *
 * <p>식단표 셀은 보통 "새우가득새우링/나쵸샐러드", "콘푸로스트&우유" 처럼
 * 여러 메뉴를 구분자로 묶어 담는다. 이를 분리한다.
 */
final class MenuTokenizer {

    private static final Pattern SPLIT = Pattern.compile("[/&,()\\n\\r·]+");

    private MenuTokenizer() {
    }

    static List<String> splitMenuNames(List<String> rawTokens) {
        List<String> result = new ArrayList<>();
        for (String raw : rawTokens) {
            if (raw == null) {
                continue;
            }
            for (String part : SPLIT.split(raw)) {
                String cleaned = part.trim();
                if (!cleaned.isBlank()) {
                    result.add(cleaned);
                }
            }
        }
        return result;
    }
}
