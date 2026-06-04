import { z } from 'zod';
import { MEAL_TYPE_CODES } from '@/shared/config/categories';

const currentYear = new Date().getFullYear();

/** 선택 일자: 빈 문자열 허용. 값이 있으면 1~31 정수 문자열이어야 함. */
const optionalDayString = z
  .string()
  .optional()
  .refine(
    (v) => {
      if (v == null || v.trim() === '') return true;
      const n = Number(v);
      return Number.isInteger(n) && n >= 1 && n <= 31;
    },
    { message: '1~31 사이로 입력하세요.' },
  );

/**
 * 식단 생성 폼 스키마 (입력 = 폼 필드 문자열 기준).
 * 숫자 변환은 제출 시 처리한다. RHF resolver와 타입 정합성을 위해 input/output 타입을 일치시킨다.
 */
export const GenerateFormSchema = z
  .object({
    year: z
      .string()
      .refine((v) => {
        const n = Number(v);
        return Number.isInteger(n) && n >= currentYear - 5 && n <= currentYear + 5;
      }, { message: '연도를 확인하세요.' }),
    month: z
      .string()
      .refine((v) => {
        const n = Number(v);
        return Number.isInteger(n) && n >= 1 && n <= 12;
      }, { message: '월은 1~12' }),
    startDay: optionalDayString,
    endDay: optionalDayString,
    mealTypes: z.array(z.enum(MEAL_TYPE_CODES)).min(1, '끼니를 최소 1개 선택하세요.'),
    includeWeekends: z.boolean(),
    requirements: z.string().max(500, '요구사항은 500자 이내').optional(),
    mustIncludeText: z.string().max(500).optional(),
  })
  .refine(
    (v) => {
      const s = v.startDay && v.startDay.trim() !== '' ? Number(v.startDay) : null;
      const e = v.endDay && v.endDay.trim() !== '' ? Number(v.endDay) : null;
      return s == null || e == null || s <= e;
    },
    { message: '시작일이 종료일보다 클 수 없습니다.', path: ['endDay'] },
  );

export type GenerateFormValues = z.infer<typeof GenerateFormSchema>;

/** 폼 값(문자열) → API 요청에 쓰일 정수/undefined로 변환. */
export function toDayNumber(value: string | undefined): number | undefined {
  if (value == null || value.trim() === '') return undefined;
  const n = Number(value);
  return Number.isInteger(n) ? n : undefined;
}
