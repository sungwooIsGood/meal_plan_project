import { z } from 'zod';

/**
 * 클라이언트에 노출되는 환경변수. 경계에서 파싱한다.
 * 개발 기본값은 빈 문자열("") → vite proxy(/api)를 그대로 사용.
 */
const EnvSchema = z.object({
  // 같은 오리진(프록시) 사용 시 빈 문자열. 분리 배포 시 https URL 주입.
  VITE_API_BASE_URL: z.string().default(''),
});

const parsed = EnvSchema.safeParse({
  VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
});

if (!parsed.success) {
  throw new Error(`환경변수 검증 실패: ${parsed.error.message}`);
}

export const env = parsed.data;
