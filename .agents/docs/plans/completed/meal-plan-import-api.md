# Plan — 식단표 엑셀 Import API (백엔드 1차)

## 목표 (수용 기준 = 검증 입력)
- `POST /api/meal-plans/import` 멀티파트 엑셀(.xlsx) 업로드 1개 API.
- 엑셀에서 모든 텍스트를 추출 → AI(분류 포트)로 7개 카테고리로 분류 → DB 저장.
- 카테고리: STAPLE_FOOD(주식), SOUP(국/탕), MAIN_DISH(주찬), SIDE_DISH(부찬), KIMCHI(김치), DESSERT(후식), BEVERAGE(음료).
- 비-엑셀 / 깨진 파일 → 400.
- 유효 xlsx지만 급식 내용이 아님(분류 가능한 메뉴 0개) → 422.

## DB 결정: MongoDB (document)
- 하루치 식단 = 가변 길이의 카테고리별 메뉴 묶음 → 문서 모델 적합.
- 조인 불필요, "파일 업로드 단위 문서" 저장/조회 패턴.
- 테스트: flapdoodle embedded mongo (Docker 미사용 환경).

## 아키텍처 (hexagonal + DDD), context = `mealplan`
- domain/model: MenuCategory, MenuItem(VO), MealPlan(Aggregate Root), MealPlanId
- domain/port/in: ImportMealPlanUseCase
- domain/port/out: MealPlanFileReaderPort, MenuClassifierPort, MealPlanRepositoryPort
- application/service: ImportMealPlanService
- adapter/in/web: MealPlanController, dto, GlobalExceptionHandler
- adapter/out/excel: PoiExcelReader
- adapter/out/ai: RuleBasedMenuClassifier(기본/fallback), OpenAiMenuClassifier(프로퍼티/키 있을 때)
- adapter/out/persistence: MealPlanDocument, MealPlanMongoRepository, MealPlanPersistenceAdapter

## 검증 계획
- domain 단위테스트: MenuItem/ MealPlan 불변식(빈 이름 거부, 분류 메뉴 0개 → NotMealPlanContentException), MenuCategory.
- application 테스트: fake reader/classifier/repo로 오케스트레이션 + 예외 전파.
- web slice(MockMvc): 200 / 400(비xlsx) / 422(비급식 내용).
- excel reader 테스트: POI로 메모리 xlsx 생성 후 토큰 추출.
- rule-based classifier 테스트: 스펙 예시로 분류 검증.
- persistence: flapdoodle 통합테스트(가능 시) + 매핑 단위테스트.
- ArchUnit: 의존성 방향(adapter→application→domain) 기계적 강제.

## Git
- 현재 브랜치 커밋 + 푸시 (작업 끝).
