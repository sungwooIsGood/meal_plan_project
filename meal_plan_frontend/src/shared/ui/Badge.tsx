import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';

interface BadgeProps {
  children: ReactNode;
  tone?: 'brand' | 'neutral' | 'warn';
  className?: string;
}

const TONE = {
  brand: 'bg-brand-100 text-brand-700',
  neutral: 'bg-ink-300/15 text-ink-700',
  warn: 'bg-amber-100 text-amber-700',
};

export function Badge({ children, tone = 'neutral', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        TONE[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
