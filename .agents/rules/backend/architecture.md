# Backend Architecture Rules

## 핵심 원칙

백엔드 코드는 **헥사고날 아키텍처(Hexagonal / Ports & Adapters) + DDD** 구조를 따른다.

레이어는 4개로 나눈다: **primary → application → domain ← secondary**.

```
primary ──► application ──► domain ◄── secondary
(인바운드)     (유스케이스)      (순수)       (아웃바운드)
```

- 의존성 화살표는 **항상 domain 쪽(안쪽)** 을 향한다.
- domain은 어떤 외부 기술(웹·DB·캐시·AI·프레임워크)에도 의존하지 않는다.
- primary/secondary(어댑터)는 application이 정의한 **포트 인터페이스에만** 의존한다.

---

## 1. 레이어 구조

```
src/main/java/com/<root>/
└── <bounded-context>/
    ├── primary/                 ← [Inbound Adapter] 외부 진입점
    │   ├── rest/                ← HTTP Controller, 요청/응답 DTO
    │   │   └── dto/
    │   └── scheduler/           ← 스케줄러도 Inbound 어댑터
    │
    ├── application/             ← [Use Case] 비즈니스 흐름 조율
    │   ├── output/              ← Output Port 인터페이스 (아웃바운드 포트)
    │   └── usecase/             ← UseCase 인터페이스 (인바운드 포트) + Command/Query
    │       └── service/         ← UseCase 구현 (흐름 조율, 트랜잭션 경계)
    │
    ├── domain/                  ← [Domain] 순수 비즈니스 로직 (의존성 0)
    │   ├── entity/              ← Entity, Value Object, Aggregate
    │   ├── service/             ← Domain Service
    │   ├── event/               ← 도메인 이벤트
    │   └── exception/           ← 도메인 예외
    │
    └── secondary/               ← [Outbound Adapter] 외부 시스템 연동
        ├── persistence/         ← DB 연동
        │   ├── document/        ← 영속성 모델 (DB 매핑 전용. 도메인 모델과 분리)
        │   ├── mapper/          ← 도메인 ↔ 영속성 모델 변환
        │   └── adapter/         ← Output Port 구현체 (Repository 등)
        ├── cache/               ← 캐시 연동 (Port 구현체)
        └── external/            ← 외부 API/클라이언트 (Port 구현체)
```

### 핵심: 포트는 `application`에 둔다

이 컨벤션의 가장 중요한 규칙이다.

- **Output Port(아웃바운드)** 인터페이스 → `application/output/`
  - "DB에서 가져온다 / 저장한다", "캐시에서 읽는다", "AI로 분류한다" 같은 **외부 의존을 추상화**한 인터페이스.
  - 구현체는 `secondary/`에 위치.
- **UseCase(인바운드 포트)** 인터페이스 → `application/usecase/`
  - 외부(primary)가 호출하는 진입 계약.
  - 구현체는 `application/usecase/service/`.

> domain은 포트를 정의하지 않는다. domain은 순수 비즈니스 로직(entity·service·event·exception)만 가진다.

### 의존성 방향 (반드시 지킬 것)

```
primary    ──► application ──► domain
secondary  ──► application ──► domain
                  ▲
        (secondary는 application/output 포트를 구현)
```

- `domain`은 **순수**하다. 프레임워크/ORM/웹/캐시 어노테이션·타입 import 금지.
- `secondary`는 `application/output`의 포트를 **구현(implements)** 한다.
- `application`은 자신이 정의한 포트(`output`)를 **사용(inject)** 한다. `secondary`(구현체)를 직접 참조하지 않는다.
- `primary`는 `application/usecase`의 UseCase 인터페이스만 호출한다. domain 객체를 외부에 직접 노출하지 않는다.

### 포트 & 어댑터 예시 (프로젝트: meal_plan)

```java
// Output Port  (application/output)
public interface MealPlanRepositoryPort {
    MealPlan save(MealPlan mealPlan);
    Optional<MealPlan> findById(MealPlanId id);
}

// 구현체  (secondary/persistence/adapter)
public class MealPlanPersistenceAdapter implements MealPlanRepositoryPort {
    // MongoDB 연동, 도메인 ↔ document 매핑
}
```

```java
// UseCase 인바운드 포트  (application/usecase)
public interface ImportMealPlanUseCase {
    MealPlan importMealPlan(ImportMealPlanCommand command);
}

// 구현  (application/usecase/service)
public class ImportMealPlanService implements ImportMealPlanUseCase {
    private final MealPlanFileReaderPort fileReader;   // application/output
    private final MenuClassifierPort classifier;        // application/output
    private final MealPlanRepositoryPort repository;    // application/output
    // ...흐름 조율
}
```

### 실제 폴더 구조로 보는 이점

- **테스트 용이성**: `domain/service`와 `application/usecase/service`는 포트 인터페이스에만 의존하므로,
  실제 DB·AI·외부 시스템 없이 Mock/Fake로 단위 테스트가 가능하다.
- **기술 교체 유연성**: MongoDB를 다른 저장소로 교체해도 `MealPlanRepositoryPort` 구현체(secondary)만 바꾸면 되고,
  도메인·애플리케이션 로직은 건드리지 않는다. (분류기도 규칙기반 ↔ OpenAI 교체가 포트 구현 교체로 끝남)
- **관심사 분리**: `MealPlanDocument`(영속성 모델)는 DB 매핑에만 집중하고,
  `MealPlan`(도메인)은 비즈니스 규칙(불변식)에만 집중한다.
- **의존성 방향 단일화**: `primary → application → domain ← secondary` 방향으로만 의존하여
  도메인이 외부 기술에 오염되지 않는다.

---

## 2. DDD 구조

### 빌딩 블록

| 요소 | 역할 | 위치 |
|---|---|---|
| **Entity** | 식별자(ID)로 동일성을 가지는 객체 | `domain/entity/` |
| **Value Object** | 식별자가 없고 값으로 동일성을 가지는 불변 객체 | `domain/entity/` |
| **Aggregate** | 일관성 경계를 가지는 Entity 묶음. **Aggregate Root**를 통해서만 접근 | `domain/entity/` |
| **Domain Service** | 단일 Entity에 속하지 않는 도메인 로직 | `domain/service/` |
| **Domain Event / Exception** | 도메인 이벤트, 도메인 예외 | `domain/event/`, `domain/exception/` |
| **UseCase (Application Service)** | 트랜잭션 단위 오케스트레이션 | 인터페이스 `application/usecase/`, 구현 `application/usecase/service/` |
| **Output Port** | 외부 의존성 추상화(저장·캐시·외부 API·AI 등) | 인터페이스 `application/output/`, 구현 `secondary/` |

### Bounded Context

- 하나의 컨텍스트 = 하나의 최상위 폴더.
- 컨텍스트 간 직접 호출 금지. 이벤트 또는 명시적 인터페이스(Anti-Corruption Layer)로만 통신.

### 도메인 모델 규칙

- 비즈니스 로직은 **반드시** 도메인 객체(Entity/VO/Aggregate)에 둔다. Service에 흘러나오면 안 된다 (Anemic Model 금지).
- 객체 생성 시점에 불변식(invariant)을 강제한다. 잘못된 상태가 만들어질 수 없게 한다.
- Setter 남용 금지. 의도 있는 메서드명(`reserve()`, `cancel()` 등)으로 상태를 바꾼다.

---

## 3. 금지 사항

- `domain` 레이어에서 프레임워크/ORM/웹/캐시 어노테이션·타입 사용 금지
  (`@Entity`, `@Service`, `@Component`, `@Document`, 영속성 어노테이션 등)
  → 영속성 모델은 `secondary/persistence/document/`의 별도 모델로 분리하고 `mapper`로 변환.
- 포트 인터페이스를 `domain`이나 `secondary`/`primary`에서 정의 금지 → **항상 `application/output`(아웃바운드) / `application/usecase`(인바운드)**.
- `application`이 `secondary`(어댑터 구현체)를 직접 참조 금지 → 포트 인터페이스로만 주입.
- `primary`(Controller 등)에서 도메인 객체 직접 노출 금지 → DTO/Response 모델 사용.
- 컨텍스트 간 도메인 객체 공유 금지.

---

## 4. 새 기능 추가 절차

1. 도메인 모델·불변식부터 정의 (`domain/entity/`, 필요 시 `domain/service/`)
2. UseCase(인바운드 포트) 인터페이스 작성 (`application/usecase/`)
3. Output Port(아웃바운드) 인터페이스 작성 (`application/output/`)
4. UseCase 구현으로 흐름 조율 (`application/usecase/service/`)
5. 어댑터 구현
   - 진입점: `primary/rest/` (Controller, DTO)
   - 외부 연동: `secondary/persistence|cache|external/` (Output Port 구현체)
6. **도메인 로직 테스트는 [testing.md](./testing.md) 참조 — 필수**
