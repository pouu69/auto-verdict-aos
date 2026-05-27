import { useState, useEffect, useCallback } from 'react';
import type { RuleReport } from '@core/types/RuleTypes.js';
import type { EncarCarBase } from '@core/types/ParsedData.js';
import { isValue } from '@core/types/FieldStatus.js';
import { orchestrateMobile } from './orchestrate-mobile';
import type { MobileOrchestratorInput } from './orchestrate-mobile';
import { OverlayPage } from './overlay/OverlayPage';
import { AndroidBridge } from './android-bridge';
import { color } from './tokens';

interface EvalState {
  report: RuleReport;
  carBase: EncarCarBase | null;
  carId: string;
  url: string;
  rawInput: MobileOrchestratorInput;
}

export function App() {
  const [state, setState] = useState<EvalState | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [alreadySaved, setAlreadySaved] = useState(false);

  useEffect(() => {
    window.receiveEncarData = (json: string) => {
      setLoading(true);
      setError(null);
      try {
        const raw: unknown = JSON.parse(json);
        if (typeof raw !== 'object' || raw === null || !('url' in raw) || !('carId' in raw)) {
          setError('잘못된 데이터 형식');
          setLoading(false);
          return;
        }
        const input = raw as MobileOrchestratorInput;
        const { parsed, facts, report } = orchestrateMobile(input);
        const carBase = isValue(parsed.raw.base) ? parsed.raw.base.value : null;
        setState({ report, carBase, carId: input.carId, url: input.url, rawInput: input });
      } catch (e) {
        setError(e instanceof Error ? e.message : '평가 중 오류가 발생했습니다');
      } finally {
        setLoading(false);
      }
    };

    window.setAlreadySaved = (saved: boolean) => {
      setAlreadySaved(saved);
    };

    window.receiveError = (json: string) => {
      try {
        const raw: unknown = JSON.parse(json);
        const message = typeof raw === 'object' && raw !== null && 'message' in raw
          ? String((raw as Record<string, unknown>).message)
          : '알 수 없는 오류';
        setError(message);
      } catch {
        setError('알 수 없는 오류');
      }
      setLoading(false);
    };

    return () => {
      window.receiveEncarData = undefined;
      window.receiveError = undefined;
      window.setAlreadySaved = undefined;
    };
  }, []);

  const handleClose = useCallback(() => {
    AndroidBridge.closeOverlay();
  }, []);

  const handleSave = useCallback(() => {
    if (!state) return;
    AndroidBridge.saveCar({
      carId: state.carId,
      url: state.url,
      title: state.carBase
        ? `${state.carBase.category.yearMonth?.slice(0, 4) ?? ''} ${state.carBase.category.manufacturerName} ${state.carBase.category.modelName}`
        : '차량 정보 없음',
      year: state.carBase?.category.yearMonth ? parseInt(state.carBase.category.yearMonth.slice(0, 4)) : null,
      mileageKm: state.carBase?.spec.mileage ?? null,
      priceWon: state.carBase?.advertisement.price ? state.carBase.advertisement.price * 10000 : null,
      fuelType: state.carBase?.spec.fuelName ?? null,
      score: state.report.score,
      verdict: state.report.verdict,
      dangerCount: state.report.results.filter((r) => r.severity === 'killer' || r.severity === 'fail').length,
      cautionCount: state.report.warns.length,
      passCount: state.report.results.filter((r) => r.severity === 'pass').length,
      unknownCount: state.report.results.filter((r) => r.severity === 'unknown').length,
      rawJson: JSON.stringify(state.rawInput),
    });
    AndroidBridge.showToast('저장되었습니다');
  }, [state]);

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        gap: '12px',
      }}>
        <div style={{
          width: '36px',
          height: '36px',
          border: `3px solid ${color.border}`,
          borderTop: `3px solid ${color.primary}`,
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }} />
        <div style={{ fontSize: '14px', color: color.textSecondary }}>분석 중...</div>
        <button
          onClick={handleClose}
          style={{
            marginTop: '8px',
            padding: '10px 24px',
            background: color.primary,
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            cursor: 'pointer',
          }}
        >
          닫기
        </button>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        padding: '24px',
        textAlign: 'center',
      }}>
        <div style={{ fontSize: '36px', marginBottom: '12px' }}>⚠️</div>
        <div style={{ fontSize: '16px', fontWeight: 600, color: color.textPrimary }}>{error}</div>
        <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
          <button
            onClick={() => {
              AndroidBridge.showToast('다시 시도합니다');
              AndroidBridge.closeOverlay();
            }}
            style={{
              padding: '10px 24px',
              background: '#fff',
              color: color.primary,
              border: `1px solid ${color.primary}`,
              borderRadius: '8px',
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            다시 시도
          </button>
          <button
            onClick={handleClose}
            style={{
              padding: '10px 24px',
              background: color.primary,
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            닫기
          </button>
        </div>
      </div>
    );
  }

  if (!state) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        background: color.background,
        color: color.textSecondary,
        fontSize: '14px',
      }}>
        데이터 대기 중...
      </div>
    );
  }

  return (
    <OverlayPage
      report={state.report}
      carBase={state.carBase}
      onClose={handleClose}
      onSave={handleSave}
      hideSave={alreadySaved}
    />
  );
}
