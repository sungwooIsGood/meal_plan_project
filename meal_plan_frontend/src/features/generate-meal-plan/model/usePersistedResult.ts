import { useCallback, useState } from 'react';
import { z } from 'zod';
import { GenerateResultSchema, type GenerateResult } from '@/entities/meal-plan';
import {
  loadFromStorage,
  saveToStorage,
  removeFromStorage,
} from '@/shared/lib/storage';

const STORAGE_KEY = 'mealplan.lastGenerateResult';

/** 저장 형태: 결과 + 어떤 조건이었는지 라벨(연/월). */
const StoredSchema = z.object({
  label: z.string(),
  result: GenerateResultSchema,
});
export type StoredGeneration = z.infer<typeof StoredSchema>;

/**
 * 마지막 생성 결과를 localStorage에 보관한다.
 * 새로고침해도 복원되며, 읽을 때 schema로 파싱해 깨진 값은 무시한다.
 */
export function usePersistedResult() {
  const [stored, setStored] = useState<StoredGeneration | null>(() =>
    loadFromStorage(STORAGE_KEY, StoredSchema),
  );

  const save = useCallback((label: string, result: GenerateResult) => {
    const value: StoredGeneration = { label, result };
    saveToStorage(STORAGE_KEY, value);
    setStored(value);
  }, []);

  const clear = useCallback(() => {
    removeFromStorage(STORAGE_KEY);
    setStored(null);
  }, []);

  return { stored, save, clear };
}
