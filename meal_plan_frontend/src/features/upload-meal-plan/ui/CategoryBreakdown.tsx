import { CATEGORY_CODES, CATEGORY_LABEL } from '@/shared/config/categories';
import { Badge } from '@/shared/ui';
import type { ItemsByCategory } from '@/entities/meal-plan';

interface CategoryBreakdownProps {
  itemsByCategory: ItemsByCategory;
}

/** 카테고리별 메뉴를 칩 목록으로 보여준다. */
export function CategoryBreakdown({ itemsByCategory }: CategoryBreakdownProps) {
  return (
    <dl className="flex flex-col gap-4">
      {CATEGORY_CODES.map((code) => {
        const items = itemsByCategory[code] ?? [];
        if (items.length === 0) return null;
        return (
          <div key={code} className="flex flex-col gap-2 sm:flex-row sm:gap-4">
            <dt className="shrink-0 sm:w-20">
              <Badge tone="brand">{CATEGORY_LABEL[code]}</Badge>
            </dt>
            <dd className="flex flex-wrap gap-1.5">
              {items.map((name, idx) => (
                <span
                  key={`${name}-${idx}`}
                  className="rounded-lg bg-ink-300/10 px-2 py-1 text-sm text-ink-700"
                >
                  {name}
                </span>
              ))}
            </dd>
          </div>
        );
      })}
    </dl>
  );
}
