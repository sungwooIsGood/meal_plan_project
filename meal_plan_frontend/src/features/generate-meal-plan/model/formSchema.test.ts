import { describe, it, expect } from 'vitest';
import { GenerateFormSchema, toDayNumber } from './formSchema';

const base = {
  year: String(new Date().getFullYear()),
  month: '6',
  mealTypes: ['BREAKFAST'] as const,
  includeWeekends: true,
};

describe('GenerateFormSchema', () => {
  it('최소 조건(연·월·끼니1)을 통과한다', () => {
    const r = GenerateFormSchema.safeParse(base);
    expect(r.success).toBe(true);
  });

  it('끼니를 하나도 선택하지 않으면 실패한다', () => {
    const r = GenerateFormSchema.safeParse({ ...base, mealTypes: [] });
    expect(r.success).toBe(false);
  });

  it('월이 13이면 실패한다', () => {
    const r = GenerateFormSchema.safeParse({ ...base, month: '13' });
    expect(r.success).toBe(false);
  });

  it('시작일이 종료일보다 크면 실패한다', () => {
    const r = GenerateFormSchema.safeParse({ ...base, startDay: '20', endDay: '5' });
    expect(r.success).toBe(false);
  });

  it('시작일 <= 종료일이면 통과한다', () => {
    const r = GenerateFormSchema.safeParse({ ...base, startDay: '5', endDay: '20' });
    expect(r.success).toBe(true);
  });

  it('빈 일자 문자열은 허용된다 (기본 규칙)', () => {
    const r = GenerateFormSchema.safeParse({ ...base, startDay: '', endDay: '' });
    expect(r.success).toBe(true);
  });
});

describe('toDayNumber', () => {
  it('빈 문자열은 undefined', () => {
    expect(toDayNumber('')).toBeUndefined();
    expect(toDayNumber(undefined)).toBeUndefined();
  });
  it('숫자 문자열은 정수로 변환', () => {
    expect(toDayNumber('15')).toBe(15);
  });
});
