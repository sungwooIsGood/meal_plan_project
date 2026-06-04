package com.mealplan.mealplan.secondary.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mealplan.mealplan.application.output.MealPlanGeneratorPort;
import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 기반 식단 생성 어댑터 (Outbound).
 *
 * <p>{@link MealPlanGeneratorPort} 구현. {@code mealplan.ai.openai.enabled=true} 일 때만 등록된다.
 * 메뉴 풀과 규칙·요구사항을 프롬프트로 주고 JSON 식단을 받아 도메인 모델로 변환한다.
 * 생성 결과는 application 계층에서 도메인 검증기로 다시 검증된다.
 */
@Component
@ConditionalOnProperty(prefix = "mealplan.ai.openai", name = "enabled", havingValue = "true")
public class OpenAiMealPlanGenerator implements MealPlanGeneratorPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiMealPlanGenerator.class);

    private final OpenAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiMealPlanGenerator(OpenAiProperties properties,
                                   RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public List<PlannedMeal> generate(List<LocalDate> dates,
                                      List<MealType> mealTypes,
                                      Map<MenuCategory, List<MenuItem>> menuPool,
                                      String requirements,
                                      List<String> mustIncludeMenus,
                                      String ruleSummary) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("OpenAI API 키가 없어 식단 생성을 건너뜁니다(결정론적 생성기로 위임).");
            return List.of();
        }
        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(dates, mealTypes, menuPool, requirements, mustIncludeMenus, ruleSummary))
                    .retrieve()
                    .body(String.class);
            return parse(responseBody, menuPool);
        } catch (Exception e) {
            log.warn("OpenAI 식단 생성 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildRequest(List<LocalDate> dates,
                                List<MealType> mealTypes,
                                Map<MenuCategory, List<MenuItem>> menuPool,
                                String requirements,
                                List<String> mustIncludeMenus,
                                String ruleSummary) throws Exception {
        StringBuilder poolText = new StringBuilder();
        menuPool.forEach((cat, items) -> {
            poolText.append(cat.name()).append("(").append(cat.koreanName()).append("): ");
            poolText.append(String.join(", ", items.stream().map(MenuItem::name).toList()));
            poolText.append("\n");
        });

        List<String> dayStrings = dates.stream().map(LocalDate::toString).toList();
        List<String> mealStrings = mealTypes.stream().map(Enum::name).toList();

        String requirementsBlock = (requirements == null || requirements.isBlank())
                ? "(요구사항 없음 — 기본 구성 규칙만 적용)"
                : requirements;

        String system = """
                너는 한국 학교 급식 식단 생성기다. 아래 "메뉴 풀"에 있는 메뉴만 사용해 식단을 짠다.
                풀에 없는 메뉴를 만들어내지 마라.

                [우선순위 — 충돌 시 위쪽이 무조건 이긴다]
                1순위) 사용자 요구사항 (사용자가 직접 입력) — 절대적으로 최우선.
                2순위) 꼭 포함할 메뉴.
                3순위) 기본 구성·중복 규칙.

                사용자 요구사항은 기본 규칙보다 강하다. 요구사항이 기본 규칙(중복 금지 일수 등)과 충돌하면,
                기본 규칙을 깨더라도 요구사항을 그대로 따라라.
                예) "석박지를 모든 끼니에 넣어줘" → 김치 4일 중복 금지 규칙을 무시하고 모든 끼니에 석박지를 넣는다.
                요구사항과 다른 식단을 만들면 실패로 간주한다.

                ===== 1순위: 사용자 요구사항 (최우선, 반드시 준수) =====
                %s
                =====================================================

                ===== 3순위: 기본 구성·중복 규칙 (요구사항과 충돌하지 않을 때만 적용) =====
                %s

                꼭 포함할 메뉴(반드시 배치): %s

                [꼭 포함할 메뉴 배치 규칙]
                - 꼭 포함할 메뉴는 여러 날짜에 나눠서 배치하라. 특정 하루에 몰아넣지 마라.
                - 같은 날(끼니 통합)에 비슷한 메뉴를 중복 배치하지 마라.
                  예: 돈가스/돈까스/가츠동/돈가스덮밥은 모두 '돈가스류'이므로 같은 날에 둘 이상 넣지 마라.
                - 표기만 다른 동일·유사 메뉴(예: '돈가스 덮밥'과 '에비가츠동')도 같은 것으로 보고 한 끼에 중복하지 마라.
                - 단, 사용자 요구사항이 "특정 메뉴를 매번/모든 끼니에"라고 하면 그 요구사항이 위 배치 규칙보다 우선한다.

                출력은 반드시 아래 JSON 형식만. 설명 금지.
                {"meals":[{"date":"YYYY-MM-DD","mealType":"BREAKFAST|LUNCH|DINNER","items":[{"name":"메뉴명","category":"CATEGORY_CODE"}]}]}
                category는 STAPLE_FOOD,SOUP,MAIN_DISH,SIDE_DISH,KIMCHI,DESSERT,BEVERAGE 중 하나.
                """.formatted(
                        requirementsBlock,
                        ruleSummary,
                        String.join(", ", mustIncludeMenus));

        String user = """
                대상 날짜: %s
                대상 끼니: %s
                메뉴 풀:
                %s
                """.formatted(String.join(", ", dayStrings),
                        String.join(", ", mealStrings),
                        poolText);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        if (properties.getTemperature() != null) {
            root.put("temperature", properties.getTemperature());
        }
        root.putObject("response_format").put("type", "json_object");
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", system);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", user);
        return objectMapper.writeValueAsString(root);
    }

    private List<PlannedMeal> parse(String responseBody,
                                    Map<MenuCategory, List<MenuItem>> menuPool) throws Exception {
        // 풀에 있는 (name->category) 빠른 조회용
        Map<String, MenuCategory> nameToCategory = new HashMap<>();
        menuPool.forEach((cat, items) -> items.forEach(i -> nameToCategory.putIfAbsent(i.name(), cat)));

        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            return List.of();
        }
        JsonNode parsed = objectMapper.readTree(content);
        List<PlannedMeal> result = new ArrayList<>();
        for (JsonNode mealNode : parsed.path("meals")) {
            String dateStr = mealNode.path("date").asText("");
            String mealTypeStr = mealNode.path("mealType").asText("");
            if (dateStr.isBlank() || mealTypeStr.isBlank()) {
                continue;
            }
            LocalDate date;
            MealType mealType;
            try {
                date = LocalDate.parse(dateStr);
                mealType = MealType.valueOf(mealTypeStr);
            } catch (Exception e) {
                continue;
            }
            List<MenuItem> items = new ArrayList<>();
            for (JsonNode itemNode : mealNode.path("items")) {
                String name = itemNode.path("name").asText("").trim();
                if (name.isBlank()) {
                    continue;
                }
                // 풀에 존재하는 메뉴만 채택 (환각 방지). 카테고리는 풀 기준으로 정정.
                MenuCategory category = nameToCategory.get(name);
                if (category == null) {
                    continue;
                }
                items.add(MenuItem.of(name, category));
            }
            if (!items.isEmpty()) {
                result.add(new PlannedMeal(date, mealType, items));
            }
        }
        return result;
    }
}
