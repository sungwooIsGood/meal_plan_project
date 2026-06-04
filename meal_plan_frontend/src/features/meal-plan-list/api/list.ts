import { getJson } from '@/shared/api/http';
import {
  MealPlanSummaryListSchema,
  type MealPlanSummary,
} from '@/entities/meal-plan';

/** 저장된 식단표 목록(요약, 최신순) 조회. */
export async function fetchMealPlanList(): Promise<MealPlanSummary[]> {
  return getJson('/api/meal-plans', MealPlanSummaryListSchema);
}
