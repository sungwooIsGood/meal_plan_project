import { useQuery } from '@tanstack/react-query';
import { fetchGenerationRules } from '../api/generate';

/** 기본 생성 규칙 안내 쿼리 (프론트 표시용). */
export function useGenerationRules() {
  return useQuery({
    queryKey: ['generation-rules'],
    queryFn: fetchGenerationRules,
    staleTime: 1000 * 60 * 60, // 규칙은 자주 안 바뀜
  });
}
