import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderWithProviders } from '@/shared/test/renderWithProviders';
import { server } from '@/shared/test/server';
import { MealPlanList } from './MealPlanList';

const PLANS = [
  { id: 'a1', sourceFileName: '6월 조식.xlsx', importedAt: '2026-06-01T09:00:00Z', totalItems: 30 },
  { id: 'b2', sourceFileName: '6월 중식.xlsx', importedAt: '2026-06-02T09:00:00Z', totalItems: 42 },
];

beforeEach(() => {
  server.use(
    http.get('/api/meal-plans', () => HttpResponse.json(PLANS)),
  );
});

describe('MealPlanList - 선택 삭제', () => {
  it('항목을 선택하지 않으면 삭제 버튼이 비활성화된다', async () => {
    renderWithProviders(<MealPlanList />);

    expect(await screen.findByText('6월 조식.xlsx')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /선택 삭제/ })).toBeDisabled();
  });

  it('체크박스로 선택 후 삭제하면 삭제 API가 호출되고 목록이 갱신된다', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    let deletedIds: string[] = [];
    let remaining = PLANS;
    server.use(
      http.get('/api/meal-plans', () => HttpResponse.json(remaining)),
      http.delete('/api/meal-plans', async ({ request }) => {
        const body = (await request.json()) as { ids: string[] };
        deletedIds = body.ids;
        remaining = PLANS.filter((p) => !body.ids.includes(p.id));
        return HttpResponse.json({ deletedCount: body.ids.length });
      }),
    );

    renderWithProviders(<MealPlanList />);

    await user.click(await screen.findByLabelText('6월 조식.xlsx 선택'));
    await user.click(screen.getByRole('button', { name: /선택 삭제 \(1\)/ }));

    await waitFor(() => expect(deletedIds).toEqual(['a1']));
    await waitFor(() =>
      expect(screen.queryByText('6월 조식.xlsx')).not.toBeInTheDocument(),
    );
    expect(screen.getByText('6월 중식.xlsx')).toBeInTheDocument();
  });

  it('확인 창에서 취소하면 삭제 API가 호출되지 않는다', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    const onDelete = vi.fn();
    server.use(
      http.delete('/api/meal-plans', () => {
        onDelete();
        return HttpResponse.json({ deletedCount: 0 });
      }),
    );

    renderWithProviders(<MealPlanList />);

    await user.click(await screen.findByLabelText('6월 조식.xlsx 선택'));
    await user.click(screen.getByRole('button', { name: /선택 삭제/ }));

    expect(onDelete).not.toHaveBeenCalled();
  });
});
