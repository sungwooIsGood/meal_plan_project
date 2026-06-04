/**
 * 백엔드 MenuCategory 코드와 한글 라벨 매핑. (단일 출처)
 * 백엔드 enum: STAPLE_FOOD, SOUP, MAIN_DISH, SIDE_DISH, KIMCHI, DESSERT, BEVERAGE
 */
export const CATEGORY_CODES = [
  'STAPLE_FOOD',
  'SOUP',
  'MAIN_DISH',
  'SIDE_DISH',
  'KIMCHI',
  'DESSERT',
  'BEVERAGE',
] as const;

export type CategoryCode = (typeof CATEGORY_CODES)[number];

export const CATEGORY_LABEL: Record<CategoryCode, string> = {
  STAPLE_FOOD: '주식',
  SOUP: '국/탕',
  MAIN_DISH: '주찬',
  SIDE_DISH: '부찬',
  KIMCHI: '김치',
  DESSERT: '후식',
  BEVERAGE: '음료',
};

export const MEAL_TYPE_CODES = ['BREAKFAST', 'LUNCH', 'DINNER'] as const;
export type MealTypeCode = (typeof MEAL_TYPE_CODES)[number];

export const MEAL_TYPE_LABEL: Record<MealTypeCode, string> = {
  BREAKFAST: '조식',
  LUNCH: '중식',
  DINNER: '석식',
};

/** 카테고리별 색상 클래스 (배경/텍스트). Tailwind 클래스는 정적 문자열로 둬야 빌드에 포함됨. */
export const CATEGORY_STYLE: Record<CategoryCode, { chip: string; dot: string }> = {
  STAPLE_FOOD: { chip: 'bg-amber-100 text-amber-800', dot: 'bg-amber-400' },
  SOUP: { chip: 'bg-sky-100 text-sky-800', dot: 'bg-sky-400' },
  MAIN_DISH: { chip: 'bg-rose-100 text-rose-800', dot: 'bg-rose-400' },
  SIDE_DISH: { chip: 'bg-lime-100 text-lime-800', dot: 'bg-lime-500' },
  KIMCHI: { chip: 'bg-red-100 text-red-800', dot: 'bg-red-500' },
  DESSERT: { chip: 'bg-violet-100 text-violet-800', dot: 'bg-violet-400' },
  BEVERAGE: { chip: 'bg-cyan-100 text-cyan-800', dot: 'bg-cyan-400' },
};

/** 카테고리 한글명에서 코드 역참조 (규칙 응답 key 매칭용). */
export function categoryCodeFromLabelKey(key: string): CategoryCode | null {
  const found = CATEGORY_CODES.find((code) => key.startsWith(code));
  return found ?? null;
}
