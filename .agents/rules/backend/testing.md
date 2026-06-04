# Backend Testing Rules

## 핵심 원칙

**도메인 로직은 반드시 테스트 코드로 검증한다.** 테스트 없는 도메인 로직은 완료된 것이 아니다.

---

## 1. 도메인 로직 테스트 의무

다음에 해당하는 코드는 **모두** 단위 테스트를 동반해야 한다.

- `domain/entity/` — Entity, Value Object, Aggregate의 비즈니스 메서드
- `domain/service/` — Domain Service
- `application/usecase/service/` — UseCase (Application Service)

테스트가 없는 PR은 미완성으로 간주한다.

---

## 2. 테스트 작성 규칙

### 도메인 모델 테스트

- **순수 단위 테스트**로 작성. 외부 의존성(DB·HTTP·프레임워크) 없이 객체만으로 실행 가능해야 한다.
- 검증 대상:
  - 정상 시나리오 (happy path)
  - 불변식 위반 시도 (예: 음수 금액으로 생성 시 예외)
  - 상태 전이 (예: 결제 완료 → 환불 가능 여부)
  - 경계값(boundary) 및 에러 케이스

### Application Service (UseCase) 테스트

- `application/output`의 Output Port는 **테스트 더블**(Fake/Stub/Mock) 로 대체.
- 실제 DB·캐시·외부 API를 띄우지 않는다 — 그건 통합 테스트 영역.
- 검증 대상: 오케스트레이션 흐름, 트랜잭션 경계, 예외 전파.

### 어댑터 테스트

- secondary(Outbound Adapter)
  - Persistence Adapter: Testcontainers 또는 in-memory/embedded DB로 통합 테스트.
  - Cache/External Adapter: 포트 계약대로 동작하는지 통합 또는 계약 테스트.
- primary(Inbound Adapter)
  - Web(rest) Adapter: 슬라이스 테스트(MockMvc, WebTestClient 등)로 직렬화·라우팅·검증 로직 확인.

---

## 3. 테스트 명명·구조

### 계층별 테스트 구조 (프로덕션 패키지 미러링)

프로덕션 코드와 **동일한 패키지 구조**로 테스트를 작성한다. 어느 계층의 테스트인지 경로만 봐도 드러나게 한다.

```
src/test/java/com/<root>/
└── <bounded-context>/
    ├── domain/
    │   ├── entity/              # 도메인 엔티티/VO/Aggregate 단위 테스트
    │   └── service/             # 도메인 서비스 단위 테스트
    ├── application/
    │   └── usecase/             # 애플리케이션(UseCase) 단위 테스트 (포트는 Fake/Mock)
    │       └── service/         #   동시성 테스트 등 흐름 검증
    ├── primary/
    │   └── rest/                # 컨트롤러 슬라이스 테스트 (MockMvc 등)
    └── secondary/               # 어댑터 통합 테스트 (영속성·캐시·외부 API)
```

- domain/application 테스트 = 순수/슬라이스 단위 테스트, secondary 테스트 = 통합 테스트로 구분된다.
- 테스트 파일은 대상 프로덕션 파일과 같은 계층 폴더에 둔다.

### 명명 규칙

- 테스트 메서드명은 **시나리오와 기대 결과**를 드러낸다.
  - `should_throw_when_amount_is_negative()`
  - `reserve_decreases_available_stock()`
- **Given–When–Then** 또는 **Arrange–Act–Assert** 구조 유지.
- 한 테스트는 한 가지 행동만 검증한다. 단정문(assert) 폭주 금지.

---

## 4. 커버리지

- 도메인 모델·도메인 서비스: **분기 커버리지(branch coverage) 90% 이상** 권장.
- 단순 커버리지 숫자보다 **불변식과 엣지 케이스가 모두 검증되는지**가 우선.
- 커버리지를 위한 테스트(getter 호출만 하는 식) 작성 금지.

---

## 5. 금지 사항

- 도메인 테스트에서 Spring/JPA 등 프레임워크 컨텍스트 띄우기 금지 — 순수 JUnit/Kotest 등으로 충분해야 한다.
- 테스트에서 `Thread.sleep()` 사용 금지 (Awaitility 등 명시적 동기화 사용).
- 운영 코드에 `@VisibleForTesting`, `public` 강등 금지 — 테스트가 어렵다면 설계가 잘못된 신호.
- 무작위(random) 입력에 의존하는 테스트 금지 — 시드 고정 또는 property-based 명시 사용.

---

## 6. 완료 기준 체크리스트

도메인 로직 변경 후 다음을 모두 만족해야 작업 완료:

- [ ] 새/변경된 도메인 메서드의 정상·예외 케이스 모두 테스트 존재
- [ ] 모든 단위 테스트 통과
- [ ] 통합 테스트 통과(어댑터를 건드린 경우)
- [ ] 테스트 실행 로그를 PR/완료 보고에 첨부
