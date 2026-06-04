import { Card } from '@/shared/ui';
import {
  CATEGORY_STYLE,
  CATEGORY_LABEL,
  categoryCodeFromLabelKey,
} from '@/shared/config/categories';
import { useGenerationRules } from '../model/useGenerationRules';

export function GenerationRulesNotice() {
  const { data, isLoading } = useGenerationRules();

  if (isLoading || !data) {
    return (
      <Card>
        <p className="text-sm text-ink-500">규칙 안내를 불러오는 중…</p>
      </Card>
    );
  }

  // 중복 금지 기간: 일수 내림차순 정렬
  const repeatEntries = Object.entries(data.noRepeatWithinDays).sort(
    ([, a], [, b]) => b - a,
  );
  const maxDays = repeatEntries.length > 0 ? repeatEntries[0]![1] : 1;

  return (
    <Card className="bg-gradient-to-br from-brand-50/80 to-white">
      <div className="flex items-center gap-2">
        <span className="grid h-7 w-7 place-items-center rounded-lg bg-brand-500 text-sm text-white">
          📋
        </span>
        <h3 className="font-bold text-ink-900">기본 생성 규칙</h3>
      </div>
      <p className="mt-2 text-sm text-ink-700">{data.notice}</p>

      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        {/* 하루 한 끼 구성 */}
        <section className="rounded-2xl border border-white/70 bg-white/70 p-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">
            하루 한 끼 구성
          </p>
          <ul className="mt-3 flex flex-col gap-2">
            {data.dailyComposition.map((line, idx) => {
              const code = categoryCodeFromLabelKey(extractCode(line));
              const dot = code ? CATEGORY_STYLE[code].dot : 'bg-ink-300';
              return (
                <li key={line} className="flex items-center gap-2.5 text-sm text-ink-700">
                  <span className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-ink-300/15 text-[11px] font-semibold text-ink-500">
                    {idx + 1}
                  </span>
                  <span className={`h-2 w-2 shrink-0 rounded-full ${dot}`} />
                  <span>{line}</span>
                </li>
              );
            })}
          </ul>
        </section>

        {/* 중복 금지 기간 */}
        <section className="rounded-2xl border border-white/70 bg-white/70 p-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">
            중복 금지 기간 <span className="text-ink-300">· 끼니 통합</span>
          </p>
          <ul className="mt-3 flex flex-col gap-2.5">
            {repeatEntries.map(([key, days]) => {
              const code = categoryCodeFromLabelKey(key);
              const style = code ? CATEGORY_STYLE[code] : null;
              const label = code ? CATEGORY_LABEL[code] : key;
              const widthPct = Math.max(12, Math.round((days / maxDays) * 100));
              return (
                <li key={key} className="flex items-center gap-3">
                  <span
                    className={`inline-flex w-14 shrink-0 justify-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                      style?.chip ?? 'bg-ink-300/15 text-ink-700'
                    }`}
                  >
                    {label}
                  </span>
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-ink-300/15">
                    <div
                      className={`h-full rounded-full ${style?.dot ?? 'bg-ink-400'}`}
                      style={{ width: `${widthPct}%` }}
                    />
                  </div>
                  <span className="w-10 shrink-0 text-right text-sm font-semibold text-ink-900">
                    {days}일
                  </span>
                </li>
              );
            })}
          </ul>
        </section>
      </div>

      {/* 끼니 규칙 */}
      <section className="mt-4 rounded-2xl border border-brand-200/60 bg-brand-50/60 p-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">
          끼니 규칙
        </p>
        <ul className="mt-2 flex flex-col gap-1.5 text-sm text-ink-700">
          {data.crossMealRules.map((line) => (
            <li key={line} className="flex items-start gap-2">
              <span className="mt-0.5 text-brand-500">✓</span>
              <span>{line}</span>
            </li>
          ))}
        </ul>
      </section>
    </Card>
  );
}

/** "주식(STAPLE_FOOD) 1개" 같은 문자열에서 카테고리 코드 부분을 추출. */
function extractCode(line: string): string {
  const match = line.match(/\(([A-Z_]+)\)/);
  return match?.[1] ?? line;
}
