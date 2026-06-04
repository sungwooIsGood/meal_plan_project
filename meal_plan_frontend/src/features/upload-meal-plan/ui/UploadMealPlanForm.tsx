import { useRef, useState } from 'react';
import { Button, Card } from '@/shared/ui';
import { ApiError } from '@/shared/api/http';
import { useUploadMealPlan } from '../model/useUploadMealPlan';
import { CategoryBreakdown } from './CategoryBreakdown';

const XLSX_EXT = '.xlsx';

export function UploadMealPlanForm() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string | null>(null);
  const { mutate, data, isPending, error, reset } = useUploadMealPlan();

  function handleFile(file: File | undefined) {
    setLocalError(null);
    reset();
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(XLSX_EXT)) {
      setLocalError('엑셀(.xlsx) 파일만 업로드할 수 있습니다.');
      setFileName(null);
      return;
    }
    setFileName(file.name);
    mutate(file);
  }

  const errorMessage = localError ?? toMessage(error);

  return (
    <div className="flex flex-col gap-5">
      <Card>
        <h2 className="text-lg font-bold text-ink-900">식단표 업로드</h2>
        <p className="mt-1 text-sm text-ink-500">
          학교 급식 엑셀(.xlsx)을 올리면 AI가 메뉴를 7개 카테고리로 분류해 저장합니다.
        </p>

        <label
          className="mt-5 flex cursor-pointer flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-ink-300/60 bg-white/50 px-6 py-10 text-center transition-colors hover:border-brand-400"
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            handleFile(e.dataTransfer.files?.[0]);
          }}
        >
          <span className="text-sm font-medium text-ink-700">
            파일을 끌어다 놓거나 클릭해서 선택
          </span>
          <span className="text-xs text-ink-500">.xlsx 형식만 지원</span>
          <input
            ref={inputRef}
            type="file"
            accept=".xlsx"
            className="sr-only"
            aria-label="엑셀 식단표 파일"
            onChange={(e) => handleFile(e.target.files?.[0])}
          />
          <Button
            type="button"
            variant="secondary"
            className="mt-2"
            onClick={() => inputRef.current?.click()}
          >
            파일 선택
          </Button>
        </label>

        {fileName && (
          <p className="mt-3 text-sm text-ink-700">
            선택한 파일: <span className="font-medium">{fileName}</span>
          </p>
        )}
        {isPending && (
          <p className="mt-3 text-sm text-brand-700" role="status">
            업로드하고 분류하는 중…
          </p>
        )}
        {errorMessage && (
          <p className="mt-3 text-sm text-red-600" role="alert">
            {errorMessage}
          </p>
        )}
      </Card>

      {data && (
        <Card>
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-ink-900">분류 결과</h3>
            <span className="text-sm text-ink-500">총 {data.totalItems}개 메뉴</span>
          </div>
          <p className="mt-1 text-sm text-ink-500">{data.sourceFileName}</p>
          <div className="mt-4">
            <CategoryBreakdown itemsByCategory={data.itemsByCategory} />
          </div>
        </Card>
      )}
    </div>
  );
}

function toMessage(error: Error | null): string | null {
  if (!error) return null;
  if (error instanceof ApiError) {
    if (error.code === 'INVALID_EXCEL_FILE') return '엑셀(.xlsx) 파일이 아니거나 손상되었습니다.';
    if (error.code === 'NOT_MEAL_PLAN_CONTENT')
      return '급식 식단표로 인식되는 메뉴가 없습니다. 파일을 확인해주세요.';
    return error.message;
  }
  return '업로드 중 오류가 발생했습니다.';
}
