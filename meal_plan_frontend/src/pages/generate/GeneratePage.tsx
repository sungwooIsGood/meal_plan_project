import { GenerateMealPlanForm, GenerationRulesNotice } from '@/features/generate-meal-plan';

export function GeneratePage() {
  return (
    <div className="flex flex-col gap-8">
      <section className="text-center">
        <p className="text-sm font-semibold text-brand-600">AI 급식 식단 생성</p>
        <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-ink-900 sm:text-4xl">
          한 번에 한 달치 식단 완성
        </h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-500">
          저장된 메뉴를 토대로 중복 없이, 규칙에 맞게 식단을 자동으로 짜드립니다.
        </p>
      </section>

      <GenerationRulesNotice />
      <GenerateMealPlanForm />
    </div>
  );
}
