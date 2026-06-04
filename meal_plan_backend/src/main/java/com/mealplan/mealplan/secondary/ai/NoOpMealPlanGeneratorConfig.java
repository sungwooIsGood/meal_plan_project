package com.mealplan.mealplan.secondary.ai;

import com.mealplan.mealplan.application.output.MealPlanGeneratorPort;
import com.mealplan.mealplan.domain.entity.MealType;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.domain.entity.PlannedMeal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI 식단 생성기가 비활성일 때 사용되는 no-op 생성기 설정.
 *
 * <p>빈 결과를 반환하여 application 계층이 결정론적 생성기(규칙 보장)로 진행하도록 한다.
 * {@link OpenAiMealPlanGenerator} 빈이 없을 때만 등록된다.
 */
@Configuration
public class NoOpMealPlanGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(MealPlanGeneratorPort.class)
    public MealPlanGeneratorPort noOpMealPlanGenerator() {
        return new MealPlanGeneratorPort() {
            @Override
            public List<PlannedMeal> generate(List<LocalDate> dates,
                                              List<MealType> mealTypes,
                                              Map<MenuCategory, List<MenuItem>> menuPool,
                                              String requirements,
                                              List<String> mustIncludeMenus,
                                              String ruleSummary) {
                return List.of();
            }
        };
    }
}
