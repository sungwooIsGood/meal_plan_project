# Frontend Architecture Rules

## 핵심 원칙

프론트엔드 코드는 **Feature-Sliced Design (FSD)** 구조를 따른다. 단방향 의존성 + 명시적 경계로 에이전트가 추론하기 쉽고 드리프트가 적은 구조.

기본 스택: **React + TypeScript** (대중적이고 학습 데이터 풍부 → 에이전트 정확도 높음).

---

## 1. 폴더 구조 (Feature-Sliced Design)

```
src/
├── app/              ← 앱 초기화 (Provider, Router, 전역 스타일)
├── pages/            ← 라우트 단위 페이지 (조립만, 로직 X)
├── widgets/          ← 여러 feature를 묶은 큰 UI 블록 (Header, Sidebar 등)
├── features/         ← 사용자 가치를 만드는 기능 단위 (LoginForm, AddToCart 등)
├── entities/         ← 비즈니스 엔티티 (User, Product 등의 모델·UI)
└── shared/           ← 재사용 가능한 유틸리티 (UI 키트, lib, api, config)
    ├── ui/           ← Button, Input 등 디자인 시스템 컴포넌트
    ├── lib/          ← 헬퍼·훅
    ├── api/          ← HTTP 클라이언트 베이스
    └── config/       ← env·상수
```

각 슬라이스(feature/entity/widget) 내부 구조:

```
features/auth/login-form/
├── ui/               ← 컴포넌트 (LoginForm.tsx)
├── model/            ← 상태·로직 (store, selector)
├── api/              ← 서버 통신 (해당 feature 전용)
└── index.ts          ← Public API (외부에서 import 가능한 것만 export)
```

---

## 2. 의존성 방향 (반드시 지킬 것)

```
app → pages → widgets → features → entities → shared
```

- 위 화살표 **반대 방향 import 금지**.
- 같은 레이어 내에서도 **다른 슬라이스 직접 import 금지** — `index.ts`(Public API)를 통해서만.
- `shared`는 어디서든 import 가능. 단 `shared`는 다른 레이어를 import할 수 없음.
- 자동 검증: `eslint-plugin-boundaries` 또는 `@feature-sliced/eslint-config` 사용.

---

## 3. 컴포넌트 작성 규칙

### 함수형 + Hooks만

- 클래스 컴포넌트 금지.
- 한 컴포넌트 = 한 파일. 파일명과 컴포넌트명 일치 (`LoginForm.tsx` → `LoginForm`).

### 책임 분리

- **Container/Presentational** 또는 **로직은 커스텀 훅, UI는 컴포넌트**로 분리.
- 컴포넌트 안에 직접 fetch·복잡한 비즈니스 로직 금지 → 훅(`useLoginForm`)으로 추출.
- 외부 API 호출은 항상 `api/` 또는 `shared/api/`를 거친다. 컴포넌트에서 `fetch` 직접 호출 금지.

### Props 타입

- 모든 props에 TypeScript 타입 명시 (`interface` 또는 `type`).
- `any` 금지. `unknown`으로 받아 좁히기.
- 옵셔널 props 남발 금지 — 필수와 선택을 명확히.

### 파일 크기

- 한 컴포넌트 파일 **300줄 이하** 권장. 넘으면 분할 신호.
- 한 훅 파일 **150줄 이하** 권장.

---

## 4. 상태 관리

### 계층별 도구 선택

| 상태 종류 | 도구 |
|---|---|
| **서버 상태** (API 응답) | TanStack Query (React Query) |
| **클라이언트 전역** (인증·테마 등) | Zustand (소형) / Redux Toolkit (대형) |
| **폼 상태** | React Hook Form |
| **로컬 컴포넌트 상태** | `useState` / `useReducer` |

### 규칙

- 서버에서 받는 데이터는 **반드시 TanStack Query** 또는 동등한 캐시 레이어로 관리. `useState` + `useEffect`로 직접 fetch 금지.
- 전역 상태는 **꼭 필요한 것만**. 컴포넌트 트리에서 prop drilling이 3단계 이상일 때 고려.
- 폼은 React Hook Form + Zod resolver로 **검증을 schema로 강제**.

---

## 5. 데이터 검증 (Parse, Don't Validate)

**모든 외부 데이터는 경계에서 파싱한다.** 내부에서는 검증 불필요.

```ts
// shared/api/user.ts
import { z } from 'zod'

const UserSchema = z.object({
  id: z.string(),
  email: z.string().email(),
  role: z.enum(['admin', 'user']),
})

export type User = z.infer<typeof UserSchema>

export async function getUser(id: string): Promise<User> {
  const res = await fetch(`/api/users/${id}`)
  const data = await res.json()
  return UserSchema.parse(data)  // ← 경계에서 한 번 파싱
}
```

- API 응답은 **반드시 Zod 등으로 파싱** 후 사용.
- URL 파라미터, localStorage, env 변수도 모두 파싱 경계 통과.
- `as any`, `// @ts-ignore` 사용 금지 (정당한 사유 + 사용자 승인 시만).

---

## 6. 라우팅

- 라우트 정의는 `pages/` 또는 `app/router/`에 모은다.
- 라우트 단위로 **code splitting** (`React.lazy`).
- 라우트 가드(인증 필요 등)는 `entities/session` + `features/auth`에 둔다.

---

## 7. 스타일링

- 권장: **Tailwind CSS** 또는 **CSS Modules**.
- 인라인 스타일(`style={...}`) 사용 최소화 — 동적 값에 한정.
- 디자인 토큰(색·간격·타이포)은 `shared/config/theme`에 모은다.
- 한 컴포넌트 = 한 스타일 단위. 전역 셀렉터는 `app/`의 글로벌 CSS에만.

---

## 8. 접근성 (a11y)

- 모든 인터랙티브 요소는 **키보드로 조작 가능**해야 함.
- 이미지에 `alt`, 폼 input에 `<label>` 또는 `aria-label` 필수.
- 색상만으로 정보 전달 금지 (색맹 대응).
- `eslint-plugin-jsx-a11y` 자동 검사.

---

## 9. 금지 사항

- ❌ 함수형 외 클래스 컴포넌트
- ❌ `any` 타입, `// @ts-ignore`
- ❌ 컴포넌트 내부에서 직접 `fetch`/`axios` 호출
- ❌ FSD 의존성 방향 위반 (`shared`가 `features` import 등)
- ❌ 슬라이스 내부 파일 직접 import (반드시 `index.ts` 통해서)
- ❌ `useState` + `useEffect`로 서버 상태 직접 관리
- ❌ 전역 상태 남용 (정말 필요한 것만)
- ❌ 인라인 SVG/이미지를 컴포넌트 안에 하드코딩 (assets로 분리)

---

## 10. 새 기능 추가 절차

1. 어느 레이어에 속하는지 결정 (entity? feature? widget?)
2. 슬라이스 폴더 생성: `ui/`, `model/`, `api/` 필요한 것만
3. 데이터 스키마 먼저 정의 (Zod) — 경계에서 파싱
4. 훅으로 로직 작성 → 컴포넌트는 UI만
5. `index.ts`에 외부 노출 항목 명시
6. 테스트 작성 — [testing.md](./testing.md) 참조
7. 보안 체크 — [security.md](./security.md) 참조
