package com.mealplan.mealplan.primary.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정 (Inbound/Web 관심사).
 *
 * <p>프론트엔드가 다른 출처(예: Vercel 도메인)에서 호출하므로 허용 출처를 명시한다.
 * 허용할 출처는 {@code CORS_ALLOWED_ORIGINS} 환경변수에 콤마로 구분해 주입한다.
 * 예) {@code CORS_ALLOWED_ORIGINS=https://my-app.vercel.app,http://localhost:3000}
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebCorsConfig(
            @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
