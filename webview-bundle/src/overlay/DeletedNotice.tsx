import { color, radius } from '../tokens';

interface DeletedNoticeProps {
  /** 저장된 매물에서 열었는지 여부. true면 "저장 목록에서 삭제" 액션을 노출한다. */
  isSaved: boolean;
  onRemoveFromSaved: () => void;
  onClose: () => void;
}

/**
 * 판매 완료/삭제된 엔카 매물을 다시 열었을 때 보여주는 안내 화면.
 * 저장 당시 스냅샷은 복원하지 않고, 매물이 사라졌음을 명확히 알린다.
 */
export function DeletedNotice({ isSaved, onRemoveFromSaved, onClose }: DeletedNoticeProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        padding: '24px',
        textAlign: 'center',
        boxSizing: 'border-box',
      }}
    >
      <div
        style={{
          width: '80px',
          height: '80px',
          borderRadius: radius.card,
          background: color.dangerBg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: '38px',
          marginBottom: '20px',
        }}
      >
        🗑️
      </div>
      <div style={{ fontSize: '18px', fontWeight: 700, color: color.textPrimary }}>
        삭제된 매물이에요
      </div>
      <div
        style={{
          fontSize: '14px',
          color: color.textSecondary,
          marginTop: '10px',
          lineHeight: 1.55,
          maxWidth: '300px',
        }}
      >
        판매가 완료되었거나 판매자가 매물을 내려서
        <br />
        더 이상 엔카에서 확인할 수 없어요.
      </div>

      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          marginTop: '28px',
          width: '100%',
          maxWidth: '300px',
        }}
      >
        {isSaved && (
          <button
            onClick={onRemoveFromSaved}
            style={{
              padding: '13px 24px',
              background: color.danger,
              color: '#fff',
              border: 'none',
              borderRadius: radius.button,
              fontSize: '15px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            저장 목록에서 삭제
          </button>
        )}
        <button
          onClick={onClose}
          style={{
            padding: '13px 24px',
            background: isSaved ? color.surface : color.primary,
            color: isSaved ? color.textSecondary : '#fff',
            border: isSaved ? `1px solid ${color.hairline}` : 'none',
            borderRadius: radius.button,
            fontSize: '15px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          닫기
        </button>
      </div>
    </div>
  );
}
