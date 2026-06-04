import { describe, it, expect, vi, afterEach } from 'vitest';
import { z } from 'zod';
import { getJson, ApiError } from './http';

const Schema = z.object({ ok: z.boolean() });

afterEach(() => {
  vi.restoreAllMocks();
});

describe('getJson', () => {
  it('성공 응답을 schema로 파싱한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify({ ok: true }), { status: 200 })),
    );
    await expect(getJson('/x', Schema)).resolves.toEqual({ ok: true });
  });

  it('에러 응답을 ApiError로 변환한다 (code 보존)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({ status: 404, error: 'MEAL_PLAN_NOT_FOUND', message: '없음' }),
            { status: 404 },
          ),
      ),
    );
    await expect(getJson('/x', Schema)).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      code: 'MEAL_PLAN_NOT_FOUND',
    });
  });

  it('JSON이 아닌 에러도 ApiError로 폴백한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('502 Bad Gateway', { status: 502 })),
    );
    const err = await getJson('/x', Schema).catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(502);
  });
});
