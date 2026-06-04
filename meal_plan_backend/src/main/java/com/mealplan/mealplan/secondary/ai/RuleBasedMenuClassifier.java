package com.mealplan.mealplan.secondary.ai;

import com.mealplan.mealplan.application.output.MenuClassifierPort;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 규칙(키워드) 기반 메뉴 분류기 (Outbound Adapter).
 *
 * <p>{@link MenuClassifierPort} 구현. 외부 AI 호출 없이 결정론적으로 동작하는 기본 구현.
 * 항상 빈으로 등록되어 fallback 역할을 한다. {@link OpenAiMenuClassifier} 가 활성화되면
 * 그쪽이 {@code @Primary} 로 우선 사용되고, AI 호출 실패 시 이 구현으로 위임된다.
 *
 * <p>분류 규칙은 우선순위 순서로 평가된다(먼저 매칭되는 카테고리 채택). 어떤 카테고리에도
 * 해당하지 않는 토큰(헤더·요일·숫자 등)은 결과에서 제외된다 → 급식이 아닌 파일은 0개가 되어
 * 도메인에서 422 로 거부된다.
 */
@Component
public class RuleBasedMenuClassifier implements MenuClassifierPort {

    private static final Pattern NUMERIC_OR_DATE =
            Pattern.compile("^[0-9\\s\\-:./~]+$");

    // 요일/식별 헤더 등 명백한 비메뉴 토큰
    private static final List<String> STOPWORDS = List.of(
            "월", "화", "수", "목", "금", "토", "일",
            "조식", "중식", "석식", "식단표", "학생", "메뉴");

    /** 우선순위 순서가 중요하다. 위에서부터 먼저 매칭. */
    private final Map<MenuCategory, List<String>> keywordsByCategory = buildKeywords();

    @Override
    public List<MenuItem> classify(List<String> tokens) {
        List<String> names = MenuTokenizer.splitMenuNames(tokens);
        List<MenuItem> items = new ArrayList<>();
        for (String name : names) {
            if (isNoise(name)) {
                continue;
            }
            MenuCategory category = classifyOne(name);
            if (category != null) {
                items.add(MenuItem.of(name, category));
            }
        }
        return items;
    }

    private boolean isNoise(String name) {
        if (name.length() < 2) {
            return true;
        }
        if (NUMERIC_OR_DATE.matcher(name).matches()) {
            return true;
        }
        return STOPWORDS.contains(name);
    }

    private MenuCategory classifyOne(String name) {
        // 김치류: 'X김치'/깍두기/석박지 등. 단, 김치볶음밥·김치찌개·김치전 처럼 조리어가 붙으면 김치 아님.
        if (isKimchi(name)) {
            return MenuCategory.KIMCHI;
        }
        for (Map.Entry<MenuCategory, List<String>> entry : keywordsByCategory.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (name.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean isKimchi(String name) {
        boolean cooked = name.contains("밥") || name.contains("찌개") || name.contains("전")
                || name.contains("볶음") || name.contains("국") || name.contains("찜");
        if (cooked) {
            return false;
        }
        return name.endsWith("김치") || name.contains("깍두기") || name.contains("석박지")
                || name.contains("알타리") || name.contains("총각김치") || name.contains("나박");
    }

    private Map<MenuCategory, List<String>> buildKeywords() {
        Map<MenuCategory, List<String>> map = new LinkedHashMap<>();

        // 음료
        map.put(MenuCategory.BEVERAGE, List.of(
                "우유", "주스", "쥬스", "음료", "두유", "라떼", "식혜", "사이다", "콜라",
                "스웨이터", "포카리", "게토레이", "에이드", "라임음료", "요구르트", "요거트"));

        // 국/탕 (찌개·스프 포함)
        map.put(MenuCategory.SOUP, List.of(
                "국밥", "찌개", "된장국", "미역국", "콩나물국", "버섯국", "어묵국", "계란국",
                "파국", "육개장", "삼계탕", "홍합탕", "떡국", "스프", "수프", "탕"));

        // 주식 (밥·면·죽 등)
        map.put(MenuCategory.STAPLE_FOOD, List.of(
                "비빔밥", "볶음밥", "쌀밥", "흑미밥", "수수밥", "잡곡밥", "콩나물밥", "도시락",
                "리조또", "리소토", "파스타", "국수", "라면", "우동", "냉면", "덮밥", "죽",
                "누룽지", "숭늉", "밥"));

        // 후식
        map.put(MenuCategory.DESSERT, List.of(
                "과일", "사과", "파인애플", "바나나", "수박", "멜론", "메론", "포도", "귤",
                "한라봉", "천혜향", "청귤", "오렌지", "키위", "딸기", "방울토마토",
                "요플레", "푸딩", "케이크", "젤리", "찰떡", "인절미", "떡"));

        // 주찬 (단백질 중심 메인)
        map.put(MenuCategory.MAIN_DISH, List.of(
                "제육", "불고기", "돈가스", "돈까스", "치킨", "닭갈비", "찜닭", "버팔로윙", "윙",
                "떡갈비", "함박", "스테이크", "잡채", "동그랑땡", "너비아니", "폭립", "갈비",
                "또띠아", "무슈", "샌드위치", "순대", "숙주불고기", "야채볶음", "김치찜", "찜"));

        // 부찬 (보조 반찬)
        map.put(MenuCategory.SIDE_DISH, List.of(
                "나물", "무침", "샐러드", "묵무침", "도토리묵", "진미채", "조림", "장아찌",
                "피클", "무생채", "생채", "전", "튀김", "스틱", "브라운", "오믈렛", "볼",
                "버섯볶음", "멸치볶음", "콘치즈", "볶음"));

        return map;
    }
}
