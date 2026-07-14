import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { color, radius } from '../tokens';

interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}

/**
 * 아래에서 올라오는 시트의 공용 껍데기 — 배경 딤, 슬라이드 애니메이션,
 * 배경 탭/ESC 닫기, 드래그 핸들. 내용은 children으로 채운다.
 */
export function BottomSheet({ open, onClose, children }: BottomSheetProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.35)',
        display: 'flex',
        alignItems: 'flex-end',
        zIndex: 1000,
        animation: 'sheetFade 0.18s ease',
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        style={{
          width: '100%',
          background: color.surface,
          borderRadius: '20px 20px 0 0',
          padding: '10px 20px 0',
          paddingBottom: 'calc(24px + env(safe-area-inset-bottom, 0px))',
          animation: 'sheetUp 0.22s cubic-bezier(0.2, 0.8, 0.2, 1)',
          maxHeight: '80vh',
          overflowY: 'auto',
        }}
      >
        <div style={{
          width: '36px',
          height: '4px',
          borderRadius: radius.pill,
          background: color.hairline,
          margin: '0 auto 16px',
        }} />
        {children}
        <style>{`
          @keyframes sheetUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
          @keyframes sheetFade { from { opacity: 0; } to { opacity: 1; } }
        `}</style>
      </div>
    </div>
  );
}
