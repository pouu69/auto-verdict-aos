interface AndroidBridgeInterface {
  saveCar(json: string): void;
  closeOverlay(): void;
  showToast(message: string): void;
  getClipboardUrl(): string | null;
}

interface Window {
  Android?: AndroidBridgeInterface;
  receiveEncarData?: (json: string) => void;
  receiveError?: (json: string) => void;
}
