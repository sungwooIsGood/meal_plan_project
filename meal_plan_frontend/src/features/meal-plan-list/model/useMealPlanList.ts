import { useQuery } from '@tanstack/react-query';
import { fetchMealPlanList } from '../api/list';

/** 저장된 식단표 목록 쿼리. */
export function useMealPlanList() {
  return useQuery({
    queryKey: ['meal-plans'],
    queryFn: fetchMealPlanList,
  });
}
