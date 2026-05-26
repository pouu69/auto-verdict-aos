export const AndroidBridge = {
  isAvailable(): boolean {
    return typeof window.Android !== 'undefined';
  },

  saveCar(data: Record<string, unknown>): void {
    window.Android?.saveCar(JSON.stringify(data));
  },

  closeOverlay(): void {
    window.Android?.closeOverlay();
  },

  showToast(message: string): void {
    window.Android?.showToast(message);
  },

  getClipboardUrl(): string | null {
    return window.Android?.getClipboardUrl() ?? null;
  },
} as const;
