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

  deleteSavedCar(carId: string): void {
    window.Android?.deleteSavedCar(carId);
  },

  showToast(message: string): void {
    window.Android?.showToast(message);
  },

  getClipboardUrl(): string | null {
    return window.Android?.getClipboardUrl() ?? null;
  },

  /**
   * Tell native the window.receive* handlers are registered. Native queues
   * payloads until this arrives, so it must be called after registration.
   */
  notifyReady(): void {
    window.Android?.onReady?.();
  },
} as const;
