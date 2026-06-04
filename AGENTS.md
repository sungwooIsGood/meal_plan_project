# Harness — Operating Contract
> 이 파일은 매 턴 항상 로드된다. 짧게 유지. 상세 룰은 아래 라우팅으로 on-demand 로드.

## 작동 모델
- Plan → Execute → Verify. 수용 기준을 먼저 정하고, 그게 검증 입력이다.
- 코드만 바꾸지 말고 실행 → 증거 수집 → 검증까지. 통과할 때까지 루프.
- 룰은 가능한 한 lint/CI/hook으로 기계적 강제. 룰 추가 = 자동검사 추가.

## 항상 지키는 것 (non-negotiable)
- 비가역 명령(rm -rf, reset --hard, force push, DB drop)·운영 변경 → 사용자 승인 후.
- 시크릿(.env, *.key, token) 커밋·출력 금지.
- 작업 끝 = 현재 브랜치 커밋 + 푸시까지.
- 완료 보고 전 self-validation 통과.

## 라우팅 (작업 시작 시 먼저 읽기)
- 백엔드(도메인/앱): .agents/rules/backend/architecture.md, testing.md
- 프론트(UI/상태): .agents/rules/frontend/architecture.md, testing.md, security.md
- Git 마무리: .agents/rules/git-workflow.md
- 완료 직전: .agents/rules/pre-completion-checklist.md

## 참조 디렉토리
- 문서: .agents/docs/
- 스킬: .agents/skills/
- 프롬프트: .agents/prompts/

## 완료 보고 형식
.agents/rules/pre-completion-checklist.md "6. 완료 보고 형식"을 그대로 따른다.
