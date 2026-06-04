import { UploadMealPlanForm } from '@/features/upload-meal-plan';
import { MealPlanList } from '@/features/meal-plan-list';

export function UploadPage() {
  return (
    <div className="flex flex-col gap-8">
      <section className="text-center">
        <p className="text-sm font-semibold text-brand-600">데이터 업로드</p>
        <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-ink-900 sm:text-4xl">
          엑셀 식단표를 올려주세요
        </h1>
        <p className="mx-auto mt-3 max-w-xl text-ink-500">
          올린 식단표의 메뉴가 AI로 분류되어, 식단 생성의 재료(메뉴 풀)가 됩니다.
        </p>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <UploadMealPlanForm />
        <MealPlanList />
      </div>
    </div>
  );
}
