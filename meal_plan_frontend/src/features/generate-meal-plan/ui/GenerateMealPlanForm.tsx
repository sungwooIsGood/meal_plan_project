import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Card } from '@/shared/ui';
import { MEAL_TYPE_CODES, MEAL_TYPE_LABEL } from '@/shared/config/categories';
import {
  GenerateFormSchema,
  toDayNumber,
  type GenerateFormValues,
} from '../model/formSchema';
import { useGenerateMealPlan } from '../model/useGenerateMealPlan';
import { usePersistedResult } from '../model/usePersistedResult';
import { GeneratedCalendar } from './GeneratedCalendar';

const now = new Date();

export function GenerateMealPlanForm() {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<GenerateFormValues>({
    resolver: zodResolver(GenerateFormSchema),
    defaultValues: {
      year: String(now.getFullYear()),
      month: String(now.getMonth() + 1),
      startDay: '',
      endDay: '',
      mealTypes: [],
      includeWeekends: false,
      requirements: '',
      mustIncludeText: '',
    },
  });

  const { mutate, data, isPending, error, reset } = useGenerateMealPlan();
  const { stored, save, clear } = usePersistedResult();
  const selectedMeals = watch('mealTypes') ?? [];
  const includeWeekends = watch('includeWeekends');

  function toggleMeal(code: (typeof MEAL_TYPE_CODES)[number]) {
    const next = selectedMeals.includes(code)
      ? selectedMeals.filter((m) => m !== code)
      : [...selectedMeals, code];
    setValue('mealTypes', next, { shouldValidate: true });
  }

  /** 초기화: localStorage 저장본 + 화면에 표시된 생성 결과를 모두 비운다. */
  function handleReset() {
    clear();
    reset();
  }

  function onSubmit(v: GenerateFormValues) {
    const mustIncludeMenus = (v.mustIncludeText ?? '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    mutate(
      {
        year: Number(v.year),
        month: Number(v.month),
        body: {
          startDay: toDayNumber(v.startDay),
          endDay: toDayNumber(v.endDay),
          mealTypes: v.mealTypes,
          includeWeekends: v.includeWeekends,
          requirements: v.requirements?.trim() || undefined,
          mustIncludeMenus: mustIncludeMenus.length > 0 ? mustIncludeMenus : undefined,
        },
      },
      {
        onSuccess: (result) => {
          save(`${v.year}년 ${v.month}월`, result);
        },
      },
    );
  }

  // 방금 생성한 결과가 있으면 그것을, 없으면 localStorage에 보관된 마지막 결과를 표시한다.
  const shownResult = data ?? stored?.result ?? null;
  const restored = !data && stored != null;

  return (
    <div className="flex flex-col gap-5">
      <Card>
        <h2 className="text-lg font-bold text-ink-900">식단 생성</h2>
        <p className="mt-1 text-sm text-ink-500">
          저장된 메뉴 풀에서 규칙에 맞게 한 달치 식단을 자동으로 짜드립니다.
        </p>

        <form className="mt-5 flex flex-col gap-5" onSubmit={handleSubmit(onSubmit)}>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label="연도" error={errors.year?.message}>
              <input type="number" className={inputCls} {...register('year')} />
            </Field>
            <Field label="월" error={errors.month?.message}>
              <input type="number" min={1} max={12} className={inputCls} {...register('month')} />
            </Field>
            <Field label="시작일 (선택)" error={errors.startDay?.message}>
              <input
                type="number"
                min={1}
                max={31}
                placeholder="1"
                className={inputCls}
                {...register('startDay')}
              />
            </Field>
            <Field label="종료일 (선택)" error={errors.endDay?.message}>
              <input
                type="number"
                min={1}
                max={31}
                placeholder="말일"
                className={inputCls}
                {...register('endDay')}
              />
            </Field>
          </div>
          <p className="-mt-2 text-xs text-ink-500">
            시작일·종료일을 비워두면 해당 월 전체(1일~말일)로 생성됩니다.
          </p>

          <label className="flex items-center gap-3 rounded-xl border border-ink-300/40 bg-white px-4 py-3">
            <input
              type="checkbox"
              className="h-4 w-4 accent-brand-500"
              {...register('includeWeekends')}
            />
            <span className="text-sm text-ink-700">
              주말(토·일) 포함
              <span className="ml-1 text-xs text-ink-500">
                {includeWeekends ? '· 주말도 식단을 생성합니다' : '· 평일(월~금)만 생성합니다'}
              </span>
            </span>
          </label>

          <fieldset>
            <legend className="text-sm font-medium text-ink-700">끼니 (복수 선택)</legend>
            <div className="mt-2 flex gap-2">
              {MEAL_TYPE_CODES.map((code) => {
                const active = selectedMeals.includes(code);
                return (
                  <button
                    key={code}
                    type="button"
                    aria-pressed={active}
                    onClick={() => toggleMeal(code)}
                    className={
                      active
                        ? 'rounded-full bg-brand-500 px-4 py-2 text-sm font-semibold text-white'
                        : 'rounded-full border border-ink-300/50 bg-white px-4 py-2 text-sm text-ink-700 hover:border-brand-400'
                    }
                  >
                    {MEAL_TYPE_LABEL[code]}
                  </button>
                );
              })}
            </div>
            {errors.mealTypes && (
              <p className="mt-1 text-xs text-red-600">{errors.mealTypes.message}</p>
            )}
          </fieldset>

          <Field
            label="요구사항 (선택) — 적은 내용이 기본 규칙보다 우선 적용됩니다"
            error={errors.requirements?.message}
          >
            <textarea
              rows={3}
              placeholder={
                '예) 석박지는 모든 끼니에 넣어줘\n' +
                '예) 김치볶음밥, 햄버거, 핫도그는 이번달에서 빼줘.\n' +
                '예) 생선 반찬은 일주일에 2번 이하로'
              }
              className={inputCls}
              {...register('requirements')}
            />
            <span className="text-xs text-ink-500">
              구체적으로 적을수록 정확합니다. "넣어줘 / 빼줘 / ~위주로 / 며칠에 한 번"처럼
              명확한 문장으로 작성하세요. 적은 요구사항은 기본 중복 규칙보다 강하게 반영됩니다.
            </span>
          </Field>

          <Field label="꼭 포함할 메뉴 (선택, 쉼표로 구분)" error={errors.mustIncludeText?.message}>
            <input
              type="text"
              placeholder="예: 제육볶음, 미역국"
              className={inputCls}
              {...register('mustIncludeText')}
            />
          </Field>

          <div className="flex items-center gap-3">
            <Button type="submit" size="lg" disabled={isPending}>
              {isPending ? '생성 중…' : '식단 생성하기'}
            </Button>
            {error && (
              <span className="text-sm text-red-600" role="alert">
                {error.message}
              </span>
            )}
          </div>
        </form>
      </Card>

      {shownResult && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between rounded-xl bg-ink-300/10 px-4 py-2">
            <span className="text-sm text-ink-600">
              {restored
                ? `이전에 생성한 결과${stored?.label ? ` (${stored.label})` : ''}를 불러왔습니다.`
                : '생성된 식단은 이 브라우저에 저장되어 새로고침해도 유지됩니다.'}
            </span>
            <button
              type="button"
              onClick={handleReset}
              className="shrink-0 rounded-full border border-ink-300/50 px-3 py-1 text-sm font-medium text-ink-600 hover:border-red-300 hover:text-red-600"
            >
              초기화
            </button>
          </div>
          <GeneratedCalendar result={shownResult} />
        </div>
      )}
    </div>
  );
}

const inputCls =
  'w-full rounded-xl border border-ink-300/50 bg-white px-3 py-2 text-sm text-ink-900 ' +
  'focus:border-brand-400 focus:outline-2 focus:outline-offset-1 focus:outline-brand-500';

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium text-ink-700">{label}</span>
      {children}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  );
}
