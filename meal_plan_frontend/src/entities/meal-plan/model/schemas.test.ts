import { describe, it, expect } from 'vitest';
import {
  MealPlanDetailSchema,
  GenerateResultSchema,
  GenerationRulesSchema,
  MealPlanSummaryListSchema,
} from './schemas';

describe('MealPlanDetailSchema', () => {
  it('정상 응답을 파싱한다', () => {
    const parsed = MealPlanDetailSchema.parse({
      id: 'abc',
      sourceFileName: '2026-06.xlsx',
      importedAt: '2026-06-02T00:00:00Z',
      totalItems: 2,
      itemsByCategory: { STAPLE_FOOD: ['흰쌀밥'], SOUP: ['미역국'] },
    });
    expect(parsed.totalItems).toBe(2);
    expect(parsed.itemsByCategory.STAPLE_FOOD).toEqual(['흰쌀밥']);
  });

  it('totalItems가 숫자가 아니면 거부한다', () => {
    expect(() =>
      MealPlanDetailSchema.parse({
        id: 'abc',
        sourceFileName: 'x.xlsx',
        importedAt: '2026-06-02',
        totalItems: 'two',
        itemsByCategory: {},
      }),
    ).toThrow();
  });
});

describe('GenerateResultSchema', () => {
  it('생성 결과를 파싱한다', () => {
    const parsed = GenerateResultSchema.parse({
      totalMeals: 1,
      usedAi: true,
      ruleViolations: [],
      days: [
        {
          date: '2026-06-01',
          meals: [{ mealType: 'BREAKFAST', itemsByCategory: { SOUP: ['미역국'] } }],
        },
      ],
    });
    expect(parsed.days[0]?.meals[0]?.mealType).toBe('BREAKFAST');
  });

  it('알 수 없는 mealType은 거부한다', () => {
    expect(() =>
      GenerateResultSchema.parse({
        totalMeals: 1,
        usedAi: false,
        ruleViolations: [],
        days: [{ date: '2026-06-01', meals: [{ mealType: 'BRUNCH', itemsByCategory: {} }] }],
      }),
    ).toThrow();
  });
});

describe('GenerationRulesSchema', () => {
  it('규칙 안내를 파싱한다', () => {
    const parsed = GenerationRulesSchema.parse({
      notice: '기본 규칙',
      dailyComposition: ['주식 1개'],
      noRepeatWithinDays: { STAPLE_FOOD: 7 },
      crossMealRules: ['같은 날 중복 금지'],
    });
    expect(parsed.noRepeatWithinDays.STAPLE_FOOD).toBe(7);
  });
});

describe('MealPlanSummaryListSchema', () => {
  it('빈 배열을 파싱한다', () => {
    expect(MealPlanSummaryListSchema.parse([])).toEqual([]);
  });
});
