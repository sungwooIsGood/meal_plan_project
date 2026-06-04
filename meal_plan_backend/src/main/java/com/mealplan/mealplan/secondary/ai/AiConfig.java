package com.mealplan.mealplan.secondary.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 분류 어댑터 관련 설정. OpenAI 프로퍼티 바인딩을 활성화한다.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiConfig {
}
