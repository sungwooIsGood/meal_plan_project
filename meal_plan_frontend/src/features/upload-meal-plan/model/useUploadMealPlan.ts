import { useMutation, useQueryClient } from '@tanstack/react-query';
import { uploadMealPlan } from '../api/upload';
import type { MealPlanDetail } from '@/entities/meal-plan';

/**
 * 엑셀 업로드 mutation. 성공 시 목록 쿼리를 무효화한다.
 */
export function useUploadMealPlan() {
  const queryClient = useQueryClient();

  return useMutation<MealPlanDetail, Error, File>({
    mutationFn: (file) => uploadMealPlan(file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] });
    },
  });
}
