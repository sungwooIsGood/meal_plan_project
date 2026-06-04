import { Card, Badge } from '@/shared/ui';
import {
  CATEGORY_CODES,
  CATEGORY_LABEL,
  MEAL_TYPE_LABEL,
  type MealTypeCode,
} from '@/shared/config/categories';
import type { GenerateResult } from '@/entities/meal-plan';

interface GeneratedCalendarProps {
  result: GenerateResult;
}

export function GeneratedCalendar({ result }: GeneratedCalendarProps) {
  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-bold text-ink-900">생성된 식단</h3>
        <div className="flex items-center gap-2">
          <Badge tone="neutral">총 {result.totalMeals}끼</Badge>
          <Badge tone={result.usedAi ? 'brand' : 'neutral'}>
            {result.usedAi ? 'AI 생성' : '규칙 기반 생성'}
          </Badge>
        </div>
      </div>

      {result.ruleViolations.length > 0 && (
        <details className="mt-3 rounded-xl bg-amber-50 p-3">
          <summary className="cursor-pointer text-sm font-medium text-amber-700">
            규칙 위반 {result.ruleViolations.length}건 (메뉴 풀 부족 등) — 자세히
          </summary>
          <ul className="mt-2 flex flex-col gap-1 text-xs text-amber-800">
            {result.ruleViolations.map((v, i) => (
              <li key={i}>· {v}</li>
            ))}
          </ul>
        </details>
      )}

      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {result.days.map((day) => (
          <div
            key={day.date}
            className="rounded-2xl border border-ink-300/20 bg-white/70 p-4"
          >
            <p className="text-sm font-semibold text-ink-900">{day.date}</p>
            <div className="mt-2 flex flex-col gap-3">
              {day.meals.map((meal) => (
                <div key={meal.mealType}>
                  <Badge tone="brand">{MEAL_TYPE_LABEL[meal.mealType as MealTypeCode]}</Badge>
                  <ul className="mt-1.5 flex flex-col gap-0.5 text-sm text-ink-700">
                    {CATEGORY_CODES.map((code) => {
                      const items = meal.itemsByCategory[code] ?? [];
                      if (items.length === 0) return null;
                      return (
                        <li key={code} className="flex gap-2">
                          <span className="shrink-0 text-xs text-ink-500">
                            {CATEGORY_LABEL[code]}
                          </span>
                          <span>{items.join(', ')}</span>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}
