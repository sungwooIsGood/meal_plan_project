package com.mealplan.mealplan.secondary.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealplan.mealplan.application.output.MenuClassifierPort;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.StapleType;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Chat Completions 기반 메뉴 분류기 (Outbound Adapter).
 *
 * <p>{@link MenuClassifierPort} 구현. {@code mealplan.ai.openai.enabled=true} 이고 API 키가 있을 때만
 * 빈으로 등록되며, 등록되면 {@link RuleBasedMenuClassifier} 대신 우선 사용된다(@Primary).
 *
 * <p>AI 호출이 실패하거나 키가 비어 있으면 규칙 기반 분류기로 안전하게 위임한다.
 */
@Component("openAiMenuClassifier")
@Primary
@ConditionalOnProperty(prefix = "mealplan.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiMenuClassifier implements MenuClassifierPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiMenuClassifier.class);

    private static final String SYSTEM_PROMPT = """
            너는 한국 학교 급식 식단표 분석기다. 입력으로 엑셀에서 추출한 텍스트 토큰 목록이 주어진다.
            학교마다 표 양식이 제각각이므로, 토큰 중에서 "실제 급식 메뉴(요리)"만 골라 아래 7개 카테고리로 분류하라.

            [메뉴가 아닌 것 — 결과에서 반드시 제외]
            - 날짜, 요일(월~일), 숫자, 칼로리/영양정보, 알레르기 유발번호(예: 1.2.5.), 비고
            - 헤더/제목/학교명, 끼니명(조식·중식·석식), "식단표" 같은 표 라벨
            - 양념·스프레드·소스·드레싱류: 잼, 버터, 시럽, 케찹, 마요네즈, 소스, 드레싱, 초코소스, 딸기잼, 양념장 등
              (빵·요리에 곁들이는 부속 양념은 메뉴가 아님 → 제외)

            [카테고리 코드와 분류 기준]
            - STAPLE_FOOD: 주식(탄수화물 중심). 밥·면·빵·죽·필라프·리조또 등. 예) 쌀밥, 잡곡밥, 볶음밥, 국수, 모닝빵
            - SOUP: 국/탕/찌개. 국물 요리. ※ 국밥류(돼지국밥·수육국밥 등)는 밥이 들어가도 SOUP로 분류. 예) 미역국, 된장찌개, 육개장, 떡국, 떡만둣국
            - MAIN_DISH: 주찬/메인. 그날의 주요리(고기·생선·메인 단백질 요리). 예) 제육볶음, 돈가스, 닭갈비, 떡갈비
            - SIDE_DISH: 부찬/사이드(보조 반찬). ※ 조림·장조림·무침·나물·전·튀김류는 재료가 고기여도 양과 무관하게 SIDE_DISH. 예) 멸치볶음, 시금치나물, 계란말이, 돈육장조림
            - KIMCHI: 김치류만. 예) 배추김치, 깍두기, 석박지, 알타리, 동치미. ※ 김치가 들어간 요리(김치볶음밥·김치찌개·고등어김치찜)는 KIMCHI 아님.
            - DESSERT: 후식. 과일, 떠먹는 요거트/빙과, 떡, 빵류 디저트·과자. 예) 사과, 바나나, 떡, 아이스크림
            - BEVERAGE: 음료. 마시는 것. 우유, 주스, 마시는 요구르트(드링크), 차, 탄산. 예) 우유, 딸기우유, 주스
              ※ 떠먹는 요구르트는 DESSERT, 마시는 요구르트는 BEVERAGE.

            [경계 판단 원칙]
            - 애매하면 "어떻게 먹는가"로 판단: 마시면 BEVERAGE, 떠먹으면 DESSERT, 밥에 곁들이는 작은 반찬이면 SIDE_DISH.
            - 한 토큰에 여러 메뉴가 슬래시(/)·&로 묶여 있으면 각각 분리해서 분류하라.

            [주식 세부 유형 — category가 STAPLE_FOOD일 때만 stapleType을 함께 판단]
            - RICE: 매일 곁들여도 되는 '일반 곡물밥'. 예) 흰쌀밥, 현미밥, 흑미밥, 잡곡밥, 수수밥, 귀리밥, 보리밥
            - DISH: 그 자체로 한 끼가 되는 '일품 주식'. 예) 볶음밥, 비빔밥, 콩나물밥, 김치볶음밥, 덮밥, 국수, 파스타, 리조또, 죽, 빵, 햄버거, 카레라이스, 김밥, 샌드위치
            - 헷갈리면: 흰쌀밥처럼 다른 반찬과 함께 먹는 '기본 밥'이면 RICE, 그 자체로 메인이 되면 DISH.
            - STAPLE_FOOD가 아니면 stapleType은 넣지 않거나 null.

            반드시 아래 JSON 형식으로만 응답하라(설명·주석 금지):
            {"items":[{"name":"메뉴명","category":"CATEGORY_CODE","stapleType":"RICE 또는 DISH 또는 null"}]}
            급식 메뉴가 하나도 없으면 {"items":[]} 로 응답하라.
            """;

    private final OpenAiProperties properties;
    private final MenuClassifierPort fallback;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiMenuClassifier(OpenAiProperties properties,
                                RuleBasedMenuClassifier fallback,
                                RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.fallback = fallback;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public List<MenuItem> classify(List<String> tokens) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("OpenAI API 키가 비어 있어 규칙 기반 분류기로 위임합니다.");
            return fallback.classify(tokens);
        }
        try {
            String userContent = String.join("\n", tokens);
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(userContent))
                    .retrieve()
                    .body(String.class);
            return parse(responseBody);
        } catch (Exception e) {
            log.warn("OpenAI 분류 실패, 규칙 기반 분류기로 fallback: {}", e.getMessage());
            return fallback.classify(tokens);
        }
    }

    private String buildRequest(String userContent) {
        ObjectMapper m = objectMapper;
        try {
            var root = m.createObjectNode();
            root.put("model", properties.getModel());
            // temperature는 설정된 경우에만 전송한다. gpt-5 계열은 비기본 temperature를 거부한다.
            if (properties.getTemperature() != null) {
                root.put("temperature", properties.getTemperature());
            }
            var responseFormat = root.putObject("response_format");
            responseFormat.put("type", "json_object");
            var messages = root.putArray("messages");
            var sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", SYSTEM_PROMPT);
            var user = messages.addObject();
            user.put("role", "user");
            user.put("content", userContent);
            return m.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 요청 직렬화 실패", e);
        }
    }

    private List<MenuItem> parse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            return List.of();
        }
        JsonNode parsed = objectMapper.readTree(content);
        JsonNode items = parsed.path("items");
        List<MenuItem> result = new ArrayList<>();
        for (JsonNode node : items) {
            String name = node.path("name").asText("").trim();
            String categoryCode = node.path("category").asText("").trim();
            if (name.isBlank() || categoryCode.isBlank()) {
                continue;
            }
            try {
                MenuCategory category = MenuCategory.valueOf(categoryCode);
                StapleType stapleType = parseStapleType(node.path("stapleType").asText(""));
                // stapleType이 null이어도 MenuItem이 주식이면 이름으로 추론(안전망).
                result.add(MenuItem.of(name, category, stapleType));
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 카테고리/빈 이름 → 스킵
            }
        }
        return result;
    }

    private StapleType parseStapleType(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (v.equals("RICE")) {
            return StapleType.RICE;
        }
        if (v.equals("DISH")) {
            return StapleType.DISH;
        }
        return null;
    }
}
