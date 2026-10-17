interface AndroidBridgeInterface {
  saveCar(json: string): void;
  closeOverlay(): void;
  deleteSavedCar(carId: string): void;
  showToast(message: string): void;
  getClipboardUrl(): string | null;
  /** Signals native that all window.receive* handlers are registered. */
  onReady?(): void;
}

interface Window {
  Android?: AndroidBridgeInterface;
  receiveEncarData?: (json: string) => void;
  receiveError?: (json: string) => void;
  receiveDeleted?: (json: string) => void;
  setAlreadySaved?: (saved: boolean) => void;
}
