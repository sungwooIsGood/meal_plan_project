import { z } from 'zod';
import { deleteJson } from '@/shared/api/http';

const DeleteResponseSchema = z.object({ deletedCount: z.number() });

/** 선택한 식단표들을 삭제하고, 삭제된 개수를 반환한다. */
export async function deleteMealPlans(ids: string[]): Promise<number> {
  const parsed = await deleteJson('/api/meal-plans', { ids }, DeleteResponseSchema);
  return parsed.deletedCount;
}
