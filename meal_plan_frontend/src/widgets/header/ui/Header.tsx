import { NavLink } from 'react-router-dom';
import { cn } from '@/shared/lib/cn';

const NAV = [
  { to: '/', label: '식단 생성' },
  { to: '/upload', label: '식단표 업로드' },
];

export function Header() {
  return (
    <header className="sticky top-0 z-10 border-b border-white/50 bg-white/70 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-5">
        <NavLink to="/" className="flex items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-xl bg-brand-500 text-white">
            🍚
          </span>
          <span className="text-lg font-extrabold tracking-tight text-ink-900">
            식단 한 끼
          </span>
        </NavLink>

        <nav className="flex items-center gap-1">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'rounded-full px-4 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-brand-100 text-brand-700'
                    : 'text-ink-700 hover:bg-ink-300/10',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
