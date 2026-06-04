import { z } from 'zod';
import { env } from '@/shared/config/env';

/** 백엔드 표준 에러 응답 스키마. */
const ApiErrorSchema = z.object({
  status: z.number(),
  error: z.string(),
  message: z.string(),
  timestamp: z.string().optional(),
});

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body: unknown = await res.json();
    const parsed = ApiErrorSchema.safeParse(body);
    if (parsed.success) {
      return new ApiError(parsed.data.status, parsed.data.error, parsed.data.message);
    }
  } catch {
    // 파싱 실패 시 일반 에러로 폴백
  }
  return new ApiError(res.status, 'UNKNOWN', `요청 실패 (HTTP ${res.status})`);
}

/** JSON GET. 응답을 schema로 파싱한다. */
export async function getJson<T>(path: string, schema: z.ZodType<T>): Promise<T> {
  const res = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
    headers: { Accept: 'application/json' },
  });
  if (!res.ok) throw await toApiError(res);
  return schema.parse(await res.json());
}

/** JSON POST. body를 JSON으로 보내고 응답을 schema로 파싱한다. */
export async function postJson<T>(
  path: string,
  body: unknown,
  schema: z.ZodType<T>,
): Promise<T> {
  const res = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body ?? {}),
  });
  if (!res.ok) throw await toApiError(res);
  return schema.parse(await res.json());
}

/** JSON DELETE. body를 JSON으로 보내고 응답을 schema로 파싱한다. */
export async function deleteJson<T>(
  path: string,
  body: unknown,
  schema: z.ZodType<T>,
): Promise<T> {
  const res = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body ?? {}),
  });
  if (!res.ok) throw await toApiError(res);
  return schema.parse(await res.json());
}

/** multipart POST (파일 업로드). */
export async function postFile<T>(
  path: string,
  formData: FormData,
  schema: z.ZodType<T>,
): Promise<T> {
  const res = await fetch(`${env.VITE_API_BASE_URL}${path}`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) throw await toApiError(res);
  return schema.parse(await res.json());
}
