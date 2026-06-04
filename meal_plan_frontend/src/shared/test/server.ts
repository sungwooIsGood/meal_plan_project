import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

/** 기본 핸들러: 생성 규칙 + 생성 결과. 테스트에서 server.use()로 오버라이드 가능. */
export const handlers = [
  http.get('/api/meal-plans/generation-rules', () =>
    HttpResponse.json({
      notice: '요구사항이 없으면 기본 규칙대로 생성합니다.',
      dailyComposition: ['주식 1개', '국 1개'],
      noRepeatWithinDays: { STAPLE_FOOD: 7, KIMCHI: 4 },
      crossMealRules: ['같은 날 끼니 간 중복 금지'],
    }),
  ),
  http.post('/api/meal-plans/generate/:year/:month', () =>
    HttpResponse.json({
      totalMeals: 1,
      usedAi: false,
      ruleViolations: [],
      days: [
        {
          date: '2026-06-01',
          meals: [
            {
              mealType: 'BREAKFAST',
              itemsByCategory: { STAPLE_FOOD: ['흰쌀밥'], SOUP: ['미역국'] },
            },
          ],
        },
      ],
    }),
  ),
];

export const server = setupServer(...handlers);
