import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteMealPlans } from '../api/delete';

/** 선택한 식단표 삭제 mutation. 성공 시 목록 쿼리를 무효화한다. */
export function useDeleteMealPlans() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: string[]) => deleteMealPlans(ids),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['meal-plans'] });
    },
  });
}
