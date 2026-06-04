# Pre-Completion Checklist

## 핵심 원칙

작업이 "끝났다"고 보고하기 **직전**에 반드시 통과해야 할 self-validation. 한 항목이라도 미흡하면 완료 보고 금지 — 보완하거나 사용자에게 한 줄 질문.

LLM의 instruction-following은 확률적이라 잊거나 패턴 매칭으로 우회할 수 있다. 결정론적 outer gate가 있어야 신뢰 가능한 완료가 된다.

---

## 0. 검증 철학 (OpenAI Harness Engineering 기반)

### 통과할 때까지 반복 (Ralph Wiggum Loop)

- 검증은 **N번**이 아니라 **"통과할 때까지"** 반복한다.
- 모든 체크리스트 항목이 충족되거나, 사람의 판단이 필요한 지점까지 자율적으로 루프.
- "더욱 분발"이 아니라 **"무엇이 빠졌는가, 어떻게 읽기/실행 가능하게 만들 것인가"**를 매번 자문.

### Plan-Execute-Verify (PEV)

- 작업 시작 시 정한 **수용 기준이 곧 검증의 입력**이다.
- 계획에 없던 수용 기준을 마지막에 추가하지 않는다 — 발견하면 계획부터 갱신.

### 수정 → 실행 → 검증 (3단계)

코드만 바꾸고 끝내지 않는다. 가능한 모든 단계를 한 번에:

1. **현재 상태 검증** — 변경 전 동작 확인
2. **재현** — 버그·요구사항 조건 재현
3. **수정** — 코드 변경
4. **실행** — 애플리케이션을 실제로 띄워 동작 확인
5. **증거 수집** — 로그·스크린샷·테스트 결과
6. **검증** — 체크리스트 통과 확인
7. **보고** — 형식에 맞춰 출력

### 기계적 강제 우선 (Mechanical Enforcement)

LLM이 "조심해" 의식하기보다 **lint·CI·hook으로 막는다**. 새 룰을 만들면 **같은 PR에서 자동 검사도 함께** 추가한다. 자동화 없는 룰은 다음 분기에 드리프트한다.

### Flaky는 후속 실행, 결정론적 실패는 즉시 수정

- 무작위·환경 의존으로 한 번 실패한 테스트는 진행을 막지 않는다 → 후속 실행/PR로 처리.
- 같은 입력에 항상 실패하는 결정론적 실패는 **반드시 그 자리에서 수정**.

---

## 1. 공통 체크 (모든 작업)

### A. 요구사항 정렬

- [ ] 사용자 요구사항을 모두 충족했는가?
- [ ] 의도적으로 범위 밖으로 둔 항목이 있다면 명시했는가?
- [ ] 새 수용 기준이 발견됐다면 계획서를 먼저 갱신했는가?

### B. 안전

- [ ] 시크릿(`.env`, `*.key`, 토큰 등)이 커밋에 섞이지 않았는가?
- [ ] 비가역 명령(`rm -rf`, `git reset --hard`, force push)을 무단으로 실행하지 않았는가?
- [ ] 외부 시스템에 비가역 변경(DB drop, 운영 데이터 변경 등)이 있다면 사용자 승인을 받았는가?

### C. 빌드·린트 (기계적 게이트)

- [ ] 빌드/컴파일 통과?
- [ ] 린트 통과?
- [ ] 타입 체크 통과 (해당 언어인 경우)?
- [ ] 새로 만든 룰이 있다면 lint·CI rule도 같은 작업에 포함했는가?

### D. 실행 검증

- [ ] 코드만 수정하고 끝내지 않고 **실행해서 동작을 확인**했는가?
- [ ] 가능한 경우 로그·스크린샷·테스트 출력 등 **증거**를 수집했는가?

### E. 보고 형식

- [ ] 무엇을, 왜, 다음 단계를 한 줄씩 명시했는가?
- [ ] 알려진 한계·미해결 항목을 명시했는가?

---

## 2. 백엔드 작업

도메인/애플리케이션 레이어를 건드린 경우:

### 아키텍처 ([backend/architecture.md](./backend/architecture.md))

- [ ] 의존성 방향이 `primary/secondary → application → domain`으로 안쪽을 향하는가?
- [ ] 도메인 레이어에 ORM·프레임워크 어노테이션이 없는가?
- [ ] 포트 인터페이스가 `application/output`(아웃바운드) / `application/usecase`(인바운드)에 정의되어 있는가?
- [ ] 어댑터(secondary/primary)가 `application`의 포트를 구현/호출하는가? (application이 어댑터 직접 참조 금지)
- [ ] 비즈니스 로직이 도메인 객체 안에 있는가 (Anemic Model 회피)?
- [ ] 컨텍스트 간 도메인 객체를 공유하지 않았는가?

### 테스트 ([backend/testing.md](./backend/testing.md))

- [ ] 새/변경된 도메인 메서드의 **정상·예외 케이스** 모두 테스트 존재?
- [ ] 도메인 단위 테스트가 프레임워크 컨텍스트 없이 순수하게 실행되는가?
- [ ] Application Service 테스트에서 외부 의존성을 테스트 더블로 대체했는가?
- [ ] 모든 단위 테스트 통과?
- [ ] 어댑터를 건드렸다면 통합 테스트도 통과?
- [ ] 테스트 실행 로그를 보고에 첨부했는가?
- [ ] flaky로 의심되는 실패는 격리하고 결정론적 실패는 즉시 수정했는가?

---

## 3. 프론트엔드 작업

UI/컴포넌트/상태 로직을 건드린 경우:

### 아키텍처 ([frontend/architecture.md](./frontend/architecture.md))

- [ ] FSD 의존성 방향 준수 (`app → pages → widgets → features → entities → shared`)?
- [ ] 슬라이스 내부 파일을 직접 import하지 않고 `index.ts`(Public API)를 통해 접근하는가?
- [ ] 컴포넌트에서 직접 `fetch`/`axios`를 호출하지 않고 `api/` 레이어를 거치는가?
- [ ] 모든 props에 TypeScript 타입이 명시되어 있는가? (`any`, `@ts-ignore` 없음)
- [ ] 외부 데이터(API·URL·localStorage)가 Zod 등으로 경계에서 파싱되는가?
- [ ] 서버 상태가 TanStack Query 등 캐시 레이어로 관리되는가? (`useEffect`+`useState` 직접 fetch 금지)

### 테스트 ([frontend/testing.md](./frontend/testing.md))

- [ ] 새/변경된 훅·model·api 단위 테스트 존재?
- [ ] 새 컴포넌트의 핵심 상호작용이 RTL로 검증되는가? (구현 디테일 X, 사용자 관점)
- [ ] 폼이 추가됐다면 Zod 스키마 검증 테스트 존재?
- [ ] 핵심 사용자 여정 변경 시 E2E(Playwright) 통과?
- [ ] `setTimeout`/`waitForTimeout` 미사용?

### 보안 ([frontend/security.md](./frontend/security.md))

- [ ] `dangerouslySetInnerHTML` 사용 시 DOMPurify로 sanitize했는가?
- [ ] 인증 토큰을 localStorage가 아닌 HttpOnly 쿠키 또는 메모리에 저장하는가?
- [ ] 클라이언트 번들에 시크릿/API 비밀키가 포함되지 않았는가?
- [ ] 모든 API 호출이 HTTPS인가?
- [ ] 외부 입력(`postMessage`, URL, localStorage)에 origin 검증 + Zod 파싱이 적용됐는가?
- [ ] 새 의존성 추가 시 `npm audit` 통과했는가?
- [ ] 보안 관련 변경에 회귀 테스트가 추가됐는가?

---

## 4. 데이터베이스 작업

(룰 정의되면 채움)

---

## 5. Git 마무리 ([git-workflow.md](./git-workflow.md))

**모든 작업의 마지막 단계**. 로컬에만 남기지 않는다.

- [ ] 현재 브랜치를 확인했는가? (`git branch --show-current`)
- [ ] 의도된 파일만 스테이징했는가? (`.env`, 비밀 파일 미포함 확인)
- [ ] 커밋 메시지가 Conventional Commits 형식인가?
- [ ] **현재 브랜치로 푸시 완료**했는가?
- [ ] 신규 브랜치라면 `-u origin <branch>`로 업스트림 설정했는가?
- [ ] pre-commit / pre-push 훅을 건너뛰지 않았는가? (`--no-verify` 사용 금지)

---

## 6. 완료 보고 형식

작업 완료 시 다음 형식으로 보고한다.

```
완료 보고
- 무엇: <한 줄>
- 왜:   <한 줄>
- 다음: <한 줄>

검증 흐름 (수정 → 실행 → 검증)
- 현재 상태: <확인 방법>
- 재현/실행: <명령 또는 시나리오>
- 증거:     <로그·스크린샷·테스트 출력 위치>

체크리스트 통과
- 공통:           ✅
- 백엔드 아키텍처: ✅ (해당 시)
- 백엔드 테스트:   ✅ (N개 추가/갱신, 모두 통과)
- 기계적 게이트:   ✅ (build / lint / typecheck / test 통과)
- Git:            ✅ (브랜치: <name>, 커밋: <hash>, 푸시: 완료)

알려진 한계
- (있으면 한 줄씩, 없으면 "없음")

후속 작업 (있을 때만)
- flaky 의심 테스트: <이름>, 후속 실행으로 확인 예정
- 범위 밖 항목:     <내용>
```

이 형식이 깨지면 **완료 미달** — 사용자가 재요청해야 한다.

---

## 7. 자동화로 대체 가능한 항목

LLM이 의식적으로 체크하기보다 **기계가 잡는 것**이 우선이다.

| 항목 | 자동화 방법 |
|---|---|
| 빌드·린트·타입 체크 통과 | `scripts/precommit.sh` + CI |
| 시크릿 누출 | `gitleaks` / `trufflehog` pre-commit |
| 의존성 레이어 위반 | ArchUnit (JVM) / dependency-cruiser (Node) / import-linter (Python) |
| 테스트 누락 시그널 | 변경 라인 기준 coverage diff |
| 커밋 메시지 형식 | commitlint / pre-commit hook |
| 문서 신선도 | doc-gardening 에이전트 + CI |
| 패턴 드리프트 | 정기 백그라운드 작업 (자동 리팩터 PR) |

**룰 추가 = 자동 검사 추가**가 원칙. 자동화로 막을 수 있는 룰은 무조건 자동화로 옮긴다. 룰 문서는 "왜"를, 자동화는 "강제"를 담당한다.

---

## 8. 룰 위반 시 메시지 패턴

커스텀 린트·검사가 룰을 잡았을 때, **수정 지침을 에러 메시지에 직접 주입**한다. 에이전트가 다음 시도에서 바로 고칠 수 있게.

좋은 예:
```
[arch-rule] domain/order/Order.kt:42 — domain 레이어에서 javax.persistence import 금지.
  → 영속성 모델은 adapter/out/persistence/OrderEntity.kt 로 분리하고 매퍼를 사용하세요.
  → 참고: .agents/rules/backend/architecture.md "금지 사항" 섹션
```

나쁜 예:
```
Architecture violation in Order.kt
```
