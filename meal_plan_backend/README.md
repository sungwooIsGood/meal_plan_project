# Meal Plan — AI 기반 식단 분류 백엔드

학교 급식 식단표 **엑셀(.xlsx)** 파일을 업로드하면, 모든 텍스트를 추출하여 AI(또는 규칙 기반)로
7개 카테고리로 분류한 뒤 MongoDB에 저장하는 백엔드.

## 기술 스택
- Java 21, Spring Boot 3.5.13, Gradle (wrapper)
- Apache POI (xlsx 파싱)
- Spring Data MongoDB (저장)
- 아키텍처: 헥사고날(Ports & Adapters) + DDD
- 테스트: JUnit5, MockMvc, ArchUnit, Testcontainers(MongoDB)

## DB 선택 — 왜 MongoDB(문서형)인가
- "업로드 1건 = 하루치(또는 한 표) 식단" 단위로 통째로 읽고 쓴다. 조인 불필요.
- 카테고리별 메뉴 개수가 가변적 → 고정 테이블/조인 테이블보다 문서가 자연스럽다.
- 메뉴 항목을 한 문서 안에 내장(embed)하여 단일 read/write로 처리.

## 카테고리 (7종)
| 코드 | 분류 | 예시 |
|---|---|---|
| `STAPLE_FOOD` | 주식 | 쌀밥, 잡곡밥, 볶음밥, 국수 |
| `SOUP` | 국/탕 | 미역국, 된장국, 육개장 |
| `MAIN_DISH` | 주찬/메인 | 제육볶음, 돈가스, 닭갈비 |
| `SIDE_DISH` | 부찬/사이드 | 멸치볶음, 시금치나물, 계란말이 |
| `KIMCHI` | 김치 | 배추김치, 깍두기 |
| `DESSERT` | 후식 | 과일, 요구르트, 떡 |
| `BEVERAGE` | 음료 | 우유, 주스 |

## API

### 식단표 업로드 (import)
```
POST /api/meal-plans/import
Content-Type: multipart/form-data
form field: file = <식단표.xlsx>
```

응답:
- `201 Created` — 분류·저장 성공
  ```json
  {
    "id": "uuid",
    "sourceFileName": "2026-06.xlsx",
    "importedAt": "2026-06-02T02:00:00Z",
    "totalItems": 30,
    "itemsByCategory": {
      "STAPLE_FOOD": ["흰쌀밥", "흑미밥", "..."],
      "SOUP": ["우삼겹된장찌개", "..."],
      "KIMCHI": ["배추김치", "..."],
      "BEVERAGE": ["우유"]
    }
  }
  ```
- `400 Bad Request` — 엑셀(.xlsx)이 아니거나 파일이 손상됨 (`error: INVALID_EXCEL_FILE`)
- `422 Unprocessable Entity` — 유효한 엑셀이지만 급식 식단표 내용이 아님(분류 가능한 메뉴 0개) (`error: NOT_MEAL_PLAN_CONTENT`)

### 식단표 조회
```
GET /api/meal-plans/{id}      # 단건 상세 (import 응답과 동일 형태). 없으면 404 MEAL_PLAN_NOT_FOUND
GET /api/meal-plans           # 목록 요약 (id, sourceFileName, importedAt, totalItems), import 최신순
```

### curl 예시
```bash
# 업로드
curl -i -F "file=@2026-06.xlsx" http://localhost:8080/api/meal-plans/import

# 업로드 응답의 id로 저장 결과 확인
curl -i http://localhost:8080/api/meal-plans/<id>

# 올린 식단표 목록
curl -i http://localhost:8080/api/meal-plans
```

## 실행
사전 요구: MongoDB. 도커로 띄우는 것을 권장한다.

```bash
# 1) MongoDB 컨테이너 기동 (docker-compose)
docker compose up -d

# 2) 앱 실행 (기본 연결: mongodb://localhost:27017/mealplan)
./gradlew bootRun
```
다른 주소를 쓰려면 `MONGODB_URI` 환경변수로 지정한다.
저장된 데이터는 `mongosh` 또는 MongoDB Compass(`mongodb://localhost:27017`)로 직접 확인할 수 있다.

## AI 분류기
- 기본값: **규칙(키워드) 기반 분류기** — 외부 의존성/시크릿 없이 동작.
- OpenAI 사용 시 환경변수로 활성화:
  ```bash
  export OPENAI_ENABLED=true
  export OPENAI_API_KEY=sk-...
  export OPENAI_MODEL=gpt-4o-mini
  ```
  실패 시 자동으로 규칙 기반으로 fallback.

## 테스트
```bash
./gradlew test
```
- 도메인 단위 테스트(순수), 애플리케이션 서비스(fake 포트), POI 리더, 규칙 분류기,
  웹 슬라이스(MockMvc, 201/400/404/422), ArchUnit(의존성 방향), MongoDB 통합, 전체 E2E.
- **DB 통합/E2E 테스트는 Testcontainers로 실제 `mongo:7.0` 컨테이너를 띄운다 → 도커 데몬이 실행 중이어야 한다.**
  (도커가 없으면 해당 테스트는 "Could not find a valid Docker environment"로 실패한다.)

## 디렉토리 구조 (헥사고날: primary → application → domain ← secondary)
```
src/main/java/com/mealplan/mealplan/
├── primary/                      ← [Inbound Adapter] 외부 진입점
│   └── rest/                     MealPlanController, GlobalExceptionHandler
│       └── dto/                  ImportMealPlanResponse, ErrorResponse
├── application/                  ← [Use Case] 비즈니스 흐름 조율
│   ├── output/                   Output Port 인터페이스
│   │                             (MealPlanFileReaderPort, MenuClassifierPort, MealPlanRepositoryPort)
│   └── usecase/                  ImportMealPlanUseCase, ImportMealPlanCommand
│       └── service/              ImportMealPlanService (UseCase 구현)
├── domain/                       ← [Domain] 순수 비즈니스 로직 (의존성 0)
│   ├── entity/                   MealPlan(Aggregate), MenuItem(VO), MenuCategory, MealPlanId
│   └── exception/                InvalidExcelFileException, NotMealPlanContentException
└── secondary/                    ← [Outbound Adapter] 외부 시스템 연동 (Output Port 구현체)
    ├── excel/                    PoiExcelReader
    ├── ai/                       RuleBasedMenuClassifier(기본), OpenAiMenuClassifier, AiConfig
    └── persistence/
        ├── document/             MealPlanDocument, MealPlanMongoRepository
        ├── mapper/               MealPlanMapper (도메인 ↔ 문서)
        └── adapter/              MealPlanPersistenceAdapter (Port 구현체)
```

테스트도 동일한 계층 구조로 미러링한다 (`domain/entity`, `application/usecase`, `primary/rest`, `secondary/*`, `architecture`).
