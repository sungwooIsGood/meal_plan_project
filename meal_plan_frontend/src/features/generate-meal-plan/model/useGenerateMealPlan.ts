import { useMutation } from '@tanstack/react-query';
import { generateMealPlan, type GenerateRequestBody } from '../api/generate';
import type { GenerateResult } from '@/entities/meal-plan';

export interface GenerateParams {
  year: number;
  month: number;
  body: GenerateRequestBody;
}

/** 식단 생성 mutation. */
export function useGenerateMealPlan() {
  return useMutation<GenerateResult, Error, GenerateParams>({
    mutationFn: ({ year, month, body }) => generateMealPlan(year, month, body),
  });
}
