import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AndroidBridge } from '../src/android-bridge';

describe('AndroidBridge', () => {
  beforeEach(() => {
    (window as unknown as Record<string, unknown>).Android = undefined;
  });

  it('detects native bridge availability', () => {
    expect(AndroidBridge.isAvailable()).toBe(false);

    (window as unknown as Record<string, unknown>).Android = {
      saveCar: vi.fn(),
      closeOverlay: vi.fn(),
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    expect(AndroidBridge.isAvailable()).toBe(true);
  });

  it('calls native saveCar with JSON', () => {
    const mockSave = vi.fn();
    (window as unknown as Record<string, unknown>).Android = {
      saveCar: mockSave,
      closeOverlay: vi.fn(),
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    AndroidBridge.saveCar({ carId: '123', score: 85 });
    expect(mockSave).toHaveBeenCalledWith(
      JSON.stringify({ carId: '123', score: 85 }),
    );
  });

  it('calls closeOverlay', () => {
    const mockClose = vi.fn();
    (window as unknown as Record<string, unknown>).Android = {
      saveCar: vi.fn(),
      closeOverlay: mockClose,
      showToast: vi.fn(),
      getClipboardUrl: vi.fn(),
    };

    AndroidBridge.closeOverlay();
    expect(mockClose).toHaveBeenCalled();
  });

  it('gracefully handles missing bridge', () => {
    expect(() => AndroidBridge.closeOverlay()).not.toThrow();
    expect(() => AndroidBridge.saveCar({ carId: '1' })).not.toThrow();
  });
});
