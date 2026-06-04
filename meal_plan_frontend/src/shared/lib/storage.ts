import type { z } from 'zod';

/**
 * 타입 안전한 localStorage 헬퍼.
 * 읽을 때 schema로 파싱(경계 검증)하여 깨지거나 변조된 값은 버린다.
 */
export function loadFromStorage<T>(key: string, schema: z.ZodType<T>): T | null {
  try {
    const raw = localStorage.getItem(key);
    if (raw == null) return null;
    const parsed = schema.safeParse(JSON.parse(raw));
    return parsed.success ? parsed.data : null;
  } catch {
    return null;
  }
}

export function saveToStorage(key: string, value: unknown): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // 용량 초과·프라이빗 모드 등 — 저장 실패해도 앱 흐름은 막지 않는다.
  }
}

export function removeFromStorage(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // noop
  }
}
