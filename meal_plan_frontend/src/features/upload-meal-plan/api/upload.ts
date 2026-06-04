import { postFile } from '@/shared/api/http';
import { MealPlanDetailSchema, type MealPlanDetail } from '@/entities/meal-plan';

/** 엑셀(.xlsx) 식단표 업로드 → 분류·저장. */
export async function uploadMealPlan(file: File): Promise<MealPlanDetail> {
  const formData = new FormData();
  formData.append('file', file);
  return postFile('/api/meal-plans/import', formData, MealPlanDetailSchema);
}
