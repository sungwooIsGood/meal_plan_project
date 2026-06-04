# Frontend Testing Rules

## 핵심 원칙

프론트엔드 테스트는 **사용자 관점**에서 작성한다. 구현 디테일이 아니라 **사용자가 보고 조작하는 것**을 검증한다.

테스트 피라미드: Unit (많이) → Component/Integration (중간) → E2E (소수, 핵심 시나리오만).

기본 도구: **Vitest + React Testing Library + Playwright**.

---

## 1. 테스트 종류 선택 트리

| 무엇을 테스트하나 | 종류 | 도구 |
|---|---|---|
| 순수 함수, 헬퍼, 훅 | Unit | Vitest |
| 컴포넌트 렌더링·상호작용 | Component | Vitest + React Testing Library |
| 여러 컴포넌트 + 상태·라우팅 | Integration | Vitest + RTL + MSW |
| 실제 사용자 시나리오 (로그인→결제 등) | E2E | Playwright |
| 디자인 회귀 | Visual | Playwright screenshot / Chromatic |

---

## 2. 무엇을 반드시 테스트하나

### 필수 (테스트 없으면 미완성)

- `features/`, `entities/`의 **로직(model/, lib/)**
- 커스텀 훅
- API 어댑터의 **Zod 파싱 경계**
- 폼 검증 스키마
- 권한·인증 가드

### 권장

- 위젯·페이지 단위 통합 테스트 (1~2개 핵심 시나리오)
- 핵심 사용자 여정 E2E (로그인, 회원가입, 결제 등)

### 권장하지 않음

- `shared/ui/`의 단순 프리젠테이셔널 컴포넌트 — 디자인 시스템 차원에서 한 번만 검증.
- 외부 라이브러리 동작 (TanStack Query 등)
- 단순 wrapper 컴포넌트

---

## 3. 컴포넌트 테스트 규칙 (React Testing Library)

### 사용자 관점 셀렉터 우선순위

```
getByRole > getByLabelText > getByPlaceholderText > getByText > getByTestId
```

- ❌ CSS 클래스명·DOM 구조로 셀렉트하지 않는다.
- ✅ 접근성 트리(role, label)로 셀렉트 → 자연스럽게 a11y도 검증됨.

### 좋은 예

```ts
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

test('로그인 폼 제출 시 에러 메시지 표시', async () => {
  const user = userEvent.setup()
  render(<LoginForm onSubmit={() => {}} />)

  await user.type(screen.getByLabelText('이메일'), 'invalid')
  await user.click(screen.getByRole('button', { name: '로그인' }))

  expect(await screen.findByText('올바른 이메일 형식이 아닙니다')).toBeInTheDocument()
})
```

### 안티 패턴

```ts
// ❌ 구현 디테일 테스트
expect(component.state.isLoading).toBe(true)
expect(wrapper.find('.btn-primary')).toHaveLength(1)

// ❌ snapshot 남발
expect(component).toMatchSnapshot()  // 회귀가 명확한 경우만 사용
```

---

## 4. API 모킹 (MSW)

서버 통신은 **MSW(Mock Service Worker)** 로 네트워크 레벨 모킹.

```ts
// shared/test/handlers.ts
import { http, HttpResponse } from 'msw'

export const handlers = [
  http.get('/api/users/:id', ({ params }) => {
    return HttpResponse.json({ id: params.id, name: 'Alice' })
  }),
]
```

- ❌ `fetch`/`axios`를 직접 jest.mock 하지 않는다.
- ✅ MSW로 실제 HTTP 레벨에서 가로채기 → 테스트와 운영 코드가 동일 경로.

---

## 5. 훅 테스트

```ts
import { renderHook, waitFor } from '@testing-library/react'

test('useUser는 사용자 정보를 가져온다', async () => {
  const { result } = renderHook(() => useUser('123'), {
    wrapper: createTestWrapper(),  // QueryClient 등 Provider
  })

  await waitFor(() => expect(result.current.data).toBeDefined())
  expect(result.current.data?.id).toBe('123')
})
```

- TanStack Query 훅 테스트 시 `QueryClient`를 매 테스트 새로 생성 (캐시 격리).

---

## 6. E2E (Playwright)

### 작성 원칙

- **핵심 사용자 여정만** 작성. 모든 UI를 E2E로 덮으려 하지 않는다.
- 실제 백엔드(또는 staging) 사용. mock으로 우회하면 E2E 의미 없음.
- 셀렉터는 RTL과 동일한 우선순위 (`getByRole` 우선).
- `page.waitForTimeout()` 절대 금지 — `waitFor`, `toBeVisible` 등 명시적 동기화만.

### 좋은 예

```ts
test('사용자가 로그인 후 대시보드로 이동', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('이메일').fill('test@example.com')
  await page.getByLabel('비밀번호').fill('password123')
  await page.getByRole('button', { name: '로그인' }).click()

  await expect(page.getByRole('heading', { name: '대시보드' })).toBeVisible()
})
```

### 안전 가드

- 운영 환경에 절대 E2E 실행 금지.
- 테스트 계정·테스트 데이터 격리.
- 시크릿은 `playwright.config.ts`가 아니라 환경변수에서 로드.

---

## 7. 보안 회귀 테스트

[security.md](./security.md)에 명시된 위험 패턴은 테스트로 회귀 방지:

- XSS 시도가 이스케이프되는지 (`<script>` 입력 → 텍스트로 렌더)
- URL 새니타이저가 `javascript:`를 차단하는지
- 인증 가드가 비로그인 사용자를 리다이렉트하는지
- Zod 스키마가 잘못된 외부 입력을 거부하는지

---

## 8. 커버리지

- 도메인 로직(`features/`, `entities/`의 model·lib): **분기 커버리지 80% 이상** 권장.
- 단순 UI 컴포넌트: 커버리지 강제하지 않음.
- 커버리지 숫자 자체보다 **엣지 케이스·에러 케이스**가 검증되는지가 우선.

---

## 9. 금지 사항

- ❌ snapshot 테스트 남발 (회귀가 명확한 경우만)
- ❌ `it.only`, `describe.only` 커밋 — ESLint로 차단
- ❌ `setTimeout`, `page.waitForTimeout` 사용
- ❌ CSS 클래스명·내부 state로 검증
- ❌ 실제 API 호출 (MSW 또는 Playwright의 격리 환경 사용)
- ❌ 테스트 간 상태 공유 (각 테스트 독립)
- ❌ 운영 환경에 E2E 실행

---

## 10. 완료 기준 체크리스트

프론트엔드 변경 후 다음을 모두 만족해야 작업 완료:

- [ ] 새/변경된 훅·model·api에 단위 테스트 존재
- [ ] 새 컴포넌트의 핵심 상호작용 컴포넌트 테스트 존재
- [ ] 폼이 추가됐다면 Zod 스키마 검증 테스트 존재
- [ ] 보안 관련 변경(XSS 가드, 인증 흐름 등)은 회귀 테스트 추가
- [ ] 모든 단위·컴포넌트 테스트 통과
- [ ] 핵심 여정에 변경이 있으면 관련 E2E 통과
- [ ] flaky 의심 실패는 격리, 결정론적 실패는 즉시 수정
- [ ] 테스트 실행 로그를 보고에 첨부
