import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { App } from '../src/App';

describe('App window callbacks', () => {
  beforeEach(() => {
    (window as unknown as Record<string, unknown>).Android = undefined;
  });

  afterEach(() => {
    window.receiveEncarData = undefined;
    window.receiveError = undefined;
  });

  it('registers receiveEncarData on mount', () => {
    render(<App />);
    expect(window.receiveEncarData).toBeDefined();
    expect(typeof window.receiveEncarData).toBe('function');
  });

  it('registers receiveError on mount', () => {
    render(<App />);
    expect(window.receiveError).toBeDefined();
  });

  it('shows loading state initially', () => {
    render(<App />);
    expect(screen.getByText('데이터 대기 중...')).toBeInTheDocument();
  });

  it('renders overlay after receiving valid data', () => {
    render(<App />);
    act(() => {
      window.receiveEncarData?.(JSON.stringify({
        url: 'https://fem.encar.com/cars/detail/123',
        carId: '123',
        preloadedState: {
          cars: {
            base: {
              category: { manufacturerName: '현대', modelName: '투싼', yearMonth: '202201', domestic: true },
              advertisement: { price: 2500 },
              spec: { mileage: 35000 },
              contact: { userType: 'DEALER' },
            },
            detailFlags: { isInsuranceExist: true, isHistoryView: true, isDiagnosisExist: false, isDealer: true },
          },
        },
      }));
    });

    expect(screen.getByText(/현대 투싼/)).toBeInTheDocument();
  });

  it('shows error for invalid JSON', () => {
    render(<App />);
    act(() => {
      window.receiveEncarData?.('not valid json');
    });

    expect(screen.getByText(/not valid JSON/)).toBeInTheDocument();
  });

  it('shows error for data missing required fields', () => {
    render(<App />);
    act(() => {
      window.receiveEncarData?.(JSON.stringify({ foo: 'bar' }));
    });

    expect(screen.getByText('잘못된 데이터 형식')).toBeInTheDocument();
  });

  it('receiveError displays error message', () => {
    render(<App />);
    act(() => {
      window.receiveError?.(JSON.stringify({ message: '네트워크 오류' }));
    });

    expect(screen.getByText('네트워크 오류')).toBeInTheDocument();
  });

  it('receiveError handles malformed JSON', () => {
    render(<App />);
    act(() => {
      window.receiveError?.('broken');
    });

    expect(screen.getByText('알 수 없는 오류')).toBeInTheDocument();
  });

  it('receiveError handles missing message field', () => {
    render(<App />);
    act(() => {
      window.receiveError?.(JSON.stringify({ code: 500 }));
    });

    expect(screen.getByText('알 수 없는 오류')).toBeInTheDocument();
  });

  it('cleans up callbacks on unmount', () => {
    const { unmount } = render(<App />);
    expect(window.receiveEncarData).toBeDefined();

    unmount();
    expect(window.receiveEncarData).toBeUndefined();
    expect(window.receiveError).toBeUndefined();
  });
});
