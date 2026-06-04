import { getJson, postJson } from '@/shared/api/http';
import {
  GenerateResultSchema,
  GenerationRulesSchema,
  type GenerateResult,
  type GenerationRulesInfo,
} from '@/entities/meal-plan';
import type { MealTypeCode } from '@/shared/config/categories';

export interface GenerateRequestBody {
  startDay?: number;
  endDay?: number;
  mealTypes?: MealTypeCode[];
  requirements?: string;
  mustIncludeMenus?: string[];
  includeWeekends?: boolean;
}

/** POST /api/meal-plans/generate/{year}/{month} */
export async function generateMealPlan(
  year: number,
  month: number,
  body: GenerateRequestBody,
): Promise<GenerateResult> {
  return postJson(`/api/meal-plans/generate/${year}/${month}`, body, GenerateResultSchema);
}

/** GET /api/meal-plans/generation-rules */
export async function fetchGenerationRules(): Promise<GenerationRulesInfo> {
  return getJson('/api/meal-plans/generation-rules', GenerationRulesSchema);
}
