package com.mealplan.mealplan.primary.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mealplan.mealplan.application.usecase.DeleteMealPlanUseCase;
import com.mealplan.mealplan.application.usecase.GetMealPlanUseCase;
import com.mealplan.mealplan.application.usecase.ImportMealPlanUseCase;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.exception.InvalidExcelFileException;
import com.mealplan.mealplan.domain.exception.MealPlanNotFoundException;
import com.mealplan.mealplan.domain.exception.NotMealPlanContentException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MealPlanController.class)
class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class StubConfig {
        @Bean
        ImportMealPlanUseCase importMealPlanUseCase() {
            return command -> {
                switch (command.originalFileName()) {
                    case "happy.xlsx" -> {
                        return MealPlan.create("happy.xlsx", List.of(
                                MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                                MenuItem.of("미역국", MenuCategory.SOUP)));
                    }
                    case "notexcel.csv" -> throw new InvalidExcelFileException("엑셀(.xlsx) 파일만 업로드할 수 있습니다.");
                    case "notmeal.xlsx" -> throw new NotMealPlanContentException("분류 가능한 급식 메뉴가 없습니다.");
                    default -> throw new IllegalStateException("unexpected: " + command.originalFileName());
                }
            };
        }

        @Bean
        GetMealPlanUseCase getMealPlanUseCase() {
            return new GetMealPlanUseCase() {
                @Override
                public MealPlan getById(String id) {
                    if ("known-id".equals(id)) {
                        return MealPlan.restore(
                                com.mealplan.mealplan.domain.entity.MealPlanId.of("known-id"),
                                "2026-06.xlsx",
                                java.time.Instant.parse("2026-06-01T00:00:00Z"),
                                List.of(
                                        MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD),
                                        MenuItem.of("배추김치", MenuCategory.KIMCHI)));
                    }
                    throw new MealPlanNotFoundException(id);
                }

                @Override
                public List<MealPlan> getAll() {
                    return List.of(MealPlan.restore(
                            com.mealplan.mealplan.domain.entity.MealPlanId.of("known-id"),
                            "2026-06.xlsx",
                            java.time.Instant.parse("2026-06-01T00:00:00Z"),
                            List.of(MenuItem.of("흰쌀밥", MenuCategory.STAPLE_FOOD))));
                }
            };
        }

        @Bean
        DeleteMealPlanUseCase deleteMealPlanUseCase() {
            return ids -> ids == null ? 0L : ids.size();
        }
    }

    @Test
    void returns_201_with_classified_items_on_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "happy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/meal-plans/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceFileName").value("happy.xlsx"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.itemsByCategory.STAPLE_FOOD[0]").value("흰쌀밥"))
                .andExpect(jsonPath("$.itemsByCategory.SOUP[0]").value("미역국"));
    }

    @Test
    void returns_400_when_not_excel() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notexcel.csv", MediaType.TEXT_PLAIN_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/meal-plans/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EXCEL_FILE"));
    }

    @Test
    void returns_422_when_not_meal_plan_content() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notmeal.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/meal-plans/import").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NOT_MEAL_PLAN_CONTENT"));
    }

    @Test
    void returns_400_when_file_part_missing() throws Exception {
        mockMvc.perform(multipart("/api/meal-plans/import"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returns_200_with_meal_plan_when_found() throws Exception {
        mockMvc.perform(get("/api/meal-plans/{id}", "known-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("known-id"))
                .andExpect(jsonPath("$.sourceFileName").value("2026-06.xlsx"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.itemsByCategory.STAPLE_FOOD[0]").value("흰쌀밥"))
                .andExpect(jsonPath("$.itemsByCategory.KIMCHI[0]").value("배추김치"));
    }

    @Test
    void get_returns_404_when_not_found() throws Exception {
        mockMvc.perform(get("/api/meal-plans/{id}", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MEAL_PLAN_NOT_FOUND"));
    }

    @Test
    void list_returns_200_with_summaries() throws Exception {
        mockMvc.perform(get("/api/meal-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("known-id"))
                .andExpect(jsonPath("$[0].sourceFileName").value("2026-06.xlsx"))
                .andExpect(jsonPath("$[0].totalItems").value(1));
    }

    @Test
    void delete_returns_200_with_deleted_count() throws Exception {
        mockMvc.perform(delete("/api/meal-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"a1\",\"b2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2));
    }
}
