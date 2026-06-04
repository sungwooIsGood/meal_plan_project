import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/shared/test/renderWithProviders';
import { GenerateMealPlanForm } from './GenerateMealPlanForm';

describe('GenerateMealPlanForm', () => {
  it('끼니를 선택하고 생성하면 생성된 식단(끼니·메뉴)이 표시된다', async () => {
    const user = userEvent.setup();
    renderWithProviders(<GenerateMealPlanForm />);

    // 기본은 아무 끼니도 선택 안 됨 → 조식 선택 후 생성
    await user.click(screen.getByRole('button', { name: '조식' }));
    await user.click(screen.getByRole('button', { name: '식단 생성하기' }));

    // MSW 기본 핸들러가 흰쌀밥/미역국을 반환 → 결과 렌더 확인
    expect(await screen.findByText('생성된 식단')).toBeInTheDocument();
    expect(screen.getByText('2026-06-01')).toBeInTheDocument();
    expect(screen.getByText(/흰쌀밥/)).toBeInTheDocument();
    expect(screen.getByText(/미역국/)).toBeInTheDocument();
  });

  it('끼니를 하나도 선택하지 않으면 검증 에러가 표시된다 (기본값: 미선택)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<GenerateMealPlanForm />);

    await user.click(screen.getByRole('button', { name: '식단 생성하기' }));

    expect(await screen.findByText('끼니를 최소 1개 선택하세요.')).toBeInTheDocument();
  });

  it('생성 결과는 localStorage에 저장되어 재마운트 시 복원된다', async () => {
    const user = userEvent.setup();
    const first = renderWithProviders(<GenerateMealPlanForm />);

    await user.click(screen.getByRole('button', { name: '조식' }));
    await user.click(screen.getByRole('button', { name: '식단 생성하기' }));
    expect(await screen.findByText('생성된 식단')).toBeInTheDocument();

    // 언마운트 후 새로 마운트(새로고침 모사) → 저장된 결과 복원
    first.unmount();
    renderWithProviders(<GenerateMealPlanForm />);

    expect(await screen.findByText(/이전에 생성한 결과/)).toBeInTheDocument();
    expect(screen.getByText('생성된 식단')).toBeInTheDocument();
  });

  it('초기화 버튼을 누르면 결과가 사라지고 재마운트해도 복원되지 않는다', async () => {
    const user = userEvent.setup();
    const first = renderWithProviders(<GenerateMealPlanForm />);

    await user.click(screen.getByRole('button', { name: '조식' }));
    await user.click(screen.getByRole('button', { name: '식단 생성하기' }));
    expect(await screen.findByText('생성된 식단')).toBeInTheDocument();

    // 초기화 → 화면에서 결과 제거
    await user.click(screen.getByRole('button', { name: '초기화' }));
    expect(screen.queryByText('생성된 식단')).not.toBeInTheDocument();

    // 재마운트해도 localStorage가 비어 복원 안 됨
    first.unmount();
    renderWithProviders(<GenerateMealPlanForm />);
    expect(screen.queryByText('생성된 식단')).not.toBeInTheDocument();
  });
});
