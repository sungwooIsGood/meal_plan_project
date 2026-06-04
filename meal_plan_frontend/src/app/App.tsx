import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryProvider } from './providers/QueryProvider';
import { Header } from '@/widgets/header';
import { GeneratePage } from '@/pages/generate/GeneratePage';
import { UploadPage } from '@/pages/upload/UploadPage';

export function App() {
  return (
    <QueryProvider>
      <BrowserRouter>
        <Header />
        <main className="mx-auto max-w-5xl px-5 py-10">
          <Routes>
            <Route path="/" element={<GeneratePage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <footer className="mx-auto max-w-5xl px-5 py-8 text-center text-xs text-ink-500">
          식단 한 끼 · AI 급식 식단 생성
        </footer>
      </BrowserRouter>
    </QueryProvider>
  );
}
