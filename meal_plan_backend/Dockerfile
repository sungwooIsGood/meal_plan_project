# syntax=docker/dockerfile:1

# ---- Build stage: Gradle + JDK 21 로 부트 JAR 생성 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1) Gradle 래퍼/빌드 스크립트 먼저 복사 → 의존성 캐시 활용
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 2) 소스 복사 후 빌드 (테스트는 빌드 단계에서 제외: Testcontainers는 도커 데몬 필요)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage: JRE 만 포함한 경량 이미지 ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 비루트 사용자로 실행 (보안)
RUN useradd -r -u 1001 appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
