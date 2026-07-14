interface AndroidBridgeInterface {
  saveCar(json: string): void;
  closeOverlay(): void;
  deleteSavedCar(carId: string): void;
  showToast(message: string): void;
  getClipboardUrl(): string | null;
}

interface Window {
  Android?: AndroidBridgeInterface;
  receiveEncarData?: (json: string) => void;
  receiveError?: (json: string) => void;
  receiveDeleted?: (json: string) => void;
  setAlreadySaved?: (saved: boolean) => void;
}
