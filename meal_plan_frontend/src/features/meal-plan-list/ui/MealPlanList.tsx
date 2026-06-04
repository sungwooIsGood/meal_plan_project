import { useState } from 'react';
import { Button, Card } from '@/shared/ui';
import { useMealPlanList } from '../model/useMealPlanList';
import { useDeleteMealPlans } from '../model/useDeleteMealPlans';

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
}

export function MealPlanList() {
  const { data, isLoading, isError } = useMealPlanList();
  const deleteMutation = useDeleteMealPlans();
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const toggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleDelete = async () => {
    const ids = [...selected];
    if (ids.length === 0) return;
    if (!window.confirm(`선택한 ${ids.length}개의 식단표를 삭제할까요?`)) return;
    await deleteMutation.mutateAsync(ids);
    setSelected(new Set());
  };

  const selectedCount = selected.size;

  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-ink-900">저장된 식단표</h2>
          <p className="mt-1 text-sm text-ink-500">
            업로드된 식단표 데이터 (생성에 사용되는 메뉴 풀)
          </p>
        </div>
        {data && data.length > 0 && (
          <Button
            variant="secondary"
            size="md"
            onClick={handleDelete}
            disabled={selectedCount === 0 || deleteMutation.isPending}
          >
            {deleteMutation.isPending ? '삭제 중…' : `선택 삭제${selectedCount > 0 ? ` (${selectedCount})` : ''}`}
          </Button>
        )}
      </div>

      {isLoading && <p className="mt-4 text-sm text-ink-500">불러오는 중…</p>}
      {isError && (
        <p className="mt-4 text-sm text-red-600" role="alert">
          목록을 불러오지 못했습니다.
        </p>
      )}
      {deleteMutation.isError && (
        <p className="mt-4 text-sm text-red-600" role="alert">
          삭제하지 못했습니다. 다시 시도해 주세요.
        </p>
      )}

      {data && data.length === 0 && (
        <p className="mt-4 text-sm text-ink-500">아직 업로드된 식단표가 없습니다.</p>
      )}

      {data && data.length > 0 && (
        <ul className="mt-4 flex flex-col divide-y divide-ink-300/20">
          {data.map((plan) => (
            <li key={plan.id} className="flex items-center gap-3 py-3">
              <input
                type="checkbox"
                className="size-4 shrink-0 accent-brand-500"
                checked={selected.has(plan.id)}
                onChange={() => toggle(plan.id)}
                aria-label={`${plan.sourceFileName} 선택`}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-ink-900">{plan.sourceFileName}</p>
                <p className="text-xs text-ink-500">{formatDate(plan.importedAt)}</p>
              </div>
              <span className="shrink-0 text-sm text-ink-700">{plan.totalItems}개</span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
