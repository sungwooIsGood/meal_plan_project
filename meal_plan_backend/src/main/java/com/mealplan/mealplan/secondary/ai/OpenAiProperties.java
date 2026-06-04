package com.mealplan.mealplan.secondary.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI 분류기 설정.
 *
 * <p>application 프로퍼티 예:
 * <pre>
 * mealplan.ai.openai.enabled=true
 * mealplan.ai.openai.api-key=${OPENAI_API_KEY}
 * mealplan.ai.openai.model=gpt-4o-mini
 * </pre>
 */
@ConfigurationProperties(prefix = "mealplan.ai.openai")
public class OpenAiProperties {

    /** OpenAI 분류기 활성화 여부. */
    private boolean enabled = false;

    /** OpenAI API 키. (시크릿 — 환경변수로 주입할 것) */
    private String apiKey;

    /** 사용 모델. */
    private String model = "gpt-5.5";

    /** Chat Completions 엔드포인트. */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 샘플링 temperature. null 이면 요청에 포함하지 않는다(모델 기본값 사용).
     * gpt-5 계열 추론 모델은 비기본 temperature(예: 0)를 거부하므로 기본 null 로 둔다.
     * gpt-4 계열에서 결정론적 분류가 필요하면 0 으로 설정.
     */
    private Double temperature;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
