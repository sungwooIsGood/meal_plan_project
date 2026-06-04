# Frontend Security Rules

## 핵심 원칙

프론트엔드는 **신뢰할 수 없는 환경**이다. 사용자 브라우저에서 실행되는 모든 코드는 변조 가능하다고 가정한다. OWASP Top 10 (특히 A03 Injection, A07 Auth Failures, A05 Misconfiguration)을 기본 가이드로 따른다.

---

## 1. XSS (Cross-Site Scripting) 방어

### React의 기본 안전성

React는 JSX의 모든 값을 자동 이스케이프한다. **하지만 다음은 위험**:

- `dangerouslySetInnerHTML` — 사용 금지가 기본. 꼭 필요하면 DOMPurify로 sanitize 후 사용.
- `<a href={userInput}>` — `javascript:` 스킴 차단 필요.
- `<iframe src={userInput}>` — 동일.

### 규칙

- ❌ `dangerouslySetInnerHTML` 무방비 사용 금지.
- ✅ HTML을 렌더링해야 하면:
  ```ts
  import DOMPurify from 'dompurify'
  <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }} />
  ```
- ✅ URL 검증 헬퍼를 `shared/lib/security/sanitize-url.ts`에 두고 모든 동적 링크 통과:
  ```ts
  function sanitizeUrl(url: string): string {
    if (/^javascript:/i.test(url) || /^data:/i.test(url)) return '#'
    return url
  }
  ```
- ✅ Markdown 렌더링은 검증된 라이브러리(`react-markdown`)만 사용. 커스텀 파서 금지.

---

## 2. CSP (Content Security Policy)

서버에서 강한 CSP 헤더를 설정한다 (백엔드 책임이지만 프론트도 호환되어야 함):

- `script-src 'self'` — 인라인 스크립트·외부 도메인 스크립트 차단.
- `style-src 'self'` — 인라인 스타일 최소화.
- `frame-ancestors 'none'` — clickjacking 방어.
- `object-src 'none'`, `base-uri 'self'`.

### 프론트 측 대응

- 인라인 스크립트(`<script>...`) 사용 금지.
- 외부 CDN 스크립트 추가 시 SRI(Subresource Integrity) 해시 포함:
  ```html
  <script src="..." integrity="sha384-..." crossorigin="anonymous"></script>
  ```
- `eval()`, `new Function()`, `setTimeout(string, ...)` 사용 금지.

---

## 3. 인증 토큰 저장

### 권장: HttpOnly + Secure 쿠키 (서버 설정)

- 액세스 토큰은 **HttpOnly·Secure·SameSite=Strict 쿠키**에 저장.
- JS에서 접근 불가 → XSS로 토큰 탈취 차단.

### localStorage / sessionStorage 사용 금지 (토큰)

- ❌ `localStorage.setItem('token', ...)` — XSS 한 방에 털림.
- 비민감 데이터(테마 설정 등)에 한정해서만 사용.

### 메모리(in-memory) 저장 시

- SPA에서 새로고침 시 사라져도 OK한 토큰만.
- refresh 흐름은 HttpOnly 쿠키로 처리.

---

## 4. CSRF 방어

- API가 **쿠키 인증**을 쓰면 CSRF 토큰 헤더(`X-CSRF-Token`) 또는 SameSite 쿠키로 방어.
- API가 **Bearer 토큰**(헤더)을 쓰면 CSRF 자동 차단됨 — 쿠키 자동 전송 안 되니까.
- 절대 GET 요청으로 상태 변경하지 않는다 (RESTful).

---

## 5. 외부 입력 검증

### 모든 경계에서 파싱 (architecture.md §5와 일관)

- 서버 응답 → Zod parse
- URL 쿼리 → Zod parse
- localStorage 값 → Zod parse
- `window.postMessage` 수신 → origin 검증 + Zod parse

```ts
window.addEventListener('message', (event) => {
  if (event.origin !== 'https://trusted.example.com') return
  const data = MessageSchema.parse(event.data)
  // ...
})
```

---

## 6. 비밀 정보 관리

### 절대 클라이언트 번들에 들어가면 안 되는 것

- API 시크릿 키
- 관리자/서버 토큰
- 데이터베이스 자격증명
- 결제 게이트웨이 비밀 키

### 환경변수

- 클라이언트 노출 변수는 **반드시 `VITE_PUBLIC_*` / `NEXT_PUBLIC_*` 같은 명시 prefix** 사용.
- `.env.local`은 git ignore.
- 빌드 산출물(JS 번들)을 검색해 시크릿 패턴이 없는지 CI에서 확인.

---

## 7. 의존성 보안

- ❌ 검증되지 않은 npm 패키지 추가 금지 — typosquatting 주의.
- ✅ 새 의존성 추가 시 다음 확인:
  - 주간 다운로드 수 (10k 이상 선호)
  - 마지막 업데이트 (1년 이내)
  - GitHub 스타·이슈 활성도
  - `npm audit` / `pnpm audit` 통과
- 자동화: Dependabot 또는 Renovate + `npm audit` CI 게이트.
- 정확한 버전 고정(lockfile) — 캐럿/틸드 범위는 패치만 허용.

---

## 8. 통신 보안

- **HTTPS 외 금지** — `http://` 호출은 dev 환경에서도 경고.
- API 베이스 URL은 `shared/config/env.ts`에 모으고 Zod로 파싱:
  ```ts
  const Env = z.object({
    VITE_API_URL: z.string().url().startsWith('https://'),
  })
  export const env = Env.parse(import.meta.env)
  ```
- CORS 설정은 서버 책임이지만, 프론트는 자격증명(`credentials: 'include'`)을 신중하게 사용.

---

## 9. 클릭재킹 방어

- 서버에서 `X-Frame-Options: DENY` 또는 `frame-ancestors 'none'` (CSP) 설정.
- 프론트는 `<iframe>`으로 외부 페이지 임베드 시 `sandbox` 속성 사용:
  ```html
  <iframe sandbox="allow-scripts allow-same-origin" src="..." />
  ```

---

## 10. 로깅·에러 노출

- 콘솔에 시크릿·PII 로그 금지 (운영 빌드).
- 에러 메시지에 스택 트레이스·내부 경로 노출 금지 — Sentry 등 외부 도구로 전송.
- `console.log` 운영 빌드 자동 제거: `babel-plugin-transform-remove-console` 또는 빌드 설정.

---

## 11. 자동화 게이트

| 항목 | 도구 |
|---|---|
| XSS 위험 패턴 (`dangerouslySetInnerHTML` 등) | ESLint custom rule |
| 의존성 취약점 | `npm audit` / `pnpm audit` CI |
| 시크릿 누출 | `gitleaks` / `trufflehog` pre-commit |
| 번들 내 시크릿 패턴 | CI에서 grep 검사 |
| CSP 호환성 | 빌드 시 인라인 스크립트 검출 |
| HTTPS 강제 | Zod env 스키마에서 `startsWith('https://')` |

---

## 12. 금지 사항 (요약)

- ❌ `dangerouslySetInnerHTML` 무방비 사용
- ❌ `eval`, `new Function`, `setTimeout(string)`
- ❌ localStorage에 인증 토큰 저장
- ❌ 시크릿 키를 클라이언트 코드에 하드코딩
- ❌ `http://` API 호출
- ❌ `<iframe>` sandbox 없이 외부 콘텐츠 임베드
- ❌ 콘솔에 PII·토큰 로그
- ❌ origin 검증 없는 `postMessage` 수신
- ❌ GET으로 상태 변경
