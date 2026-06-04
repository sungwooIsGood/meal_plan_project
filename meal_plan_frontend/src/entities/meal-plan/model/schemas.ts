import { z } from 'zod';
import { CATEGORY_CODES, MEAL_TYPE_CODES } from '@/shared/config/categories';

export const CategoryCodeSchema = z.enum(CATEGORY_CODES);
export const MealTypeCodeSchema = z.enum(MEAL_TYPE_CODES);

/**
 * 카테고리별 메뉴명 목록. 백엔드는 "항목이 있는 카테고리만" 내려주므로 부분(partial) 맵이다.
 * key는 카테고리 코드 문자열, value는 메뉴명 배열.
 */
export const ItemsByCategorySchema = z.record(z.string(), z.array(z.string()));
export type ItemsByCategory = z.infer<typeof ItemsByCategorySchema>;

/** POST /import, GET /{id} 응답. */
export const MealPlanDetailSchema = z.object({
  id: z.string(),
  sourceFileName: z.string(),
  importedAt: z.string(),
  totalItems: z.number(),
  itemsByCategory: ItemsByCategorySchema,
});
export type MealPlanDetail = z.infer<typeof MealPlanDetailSchema>;

/** GET /api/meal-plans 목록 요약. */
export const MealPlanSummarySchema = z.object({
  id: z.string(),
  sourceFileName: z.string(),
  importedAt: z.string(),
  totalItems: z.number(),
});
export type MealPlanSummary = z.infer<typeof MealPlanSummarySchema>;

export const MealPlanSummaryListSchema = z.array(MealPlanSummarySchema);

/** 생성 응답 - 한 끼. */
export const PlannedMealSchema = z.object({
  mealType: MealTypeCodeSchema,
  itemsByCategory: ItemsByCategorySchema,
});

/** 생성 응답 - 하루. */
export const DayPlanSchema = z.object({
  date: z.string(),
  meals: z.array(PlannedMealSchema),
});
export type DayPlan = z.infer<typeof DayPlanSchema>;

/** POST /generate 응답. */
export const GenerateResultSchema = z.object({
  totalMeals: z.number(),
  usedAi: z.boolean(),
  ruleViolations: z.array(z.string()),
  days: z.array(DayPlanSchema),
});
export type GenerateResult = z.infer<typeof GenerateResultSchema>;

/** GET /generation-rules 응답. */
export const GenerationRulesSchema = z.object({
  notice: z.string(),
  dailyComposition: z.array(z.string()),
  noRepeatWithinDays: z.record(z.string(), z.number()),
  crossMealRules: z.array(z.string()),
});
export type GenerationRulesInfo = z.infer<typeof GenerationRulesSchema>;
