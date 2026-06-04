import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ className, children, ...rest }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-[var(--radius-card)] border border-white/60 bg-white/80 p-6',
        'shadow-[0_10px_40px_-20px_rgba(18,20,23,0.25)] backdrop-blur-sm',
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
