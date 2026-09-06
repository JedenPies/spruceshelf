import { Component, inject, input, signal } from '@angular/core';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { BarcodeFormat } from '@zxing/library';
import { BackendService } from '../../../services/backend.service';

@Component({
  selector: 'app-scanner',
  imports: [ZXingScannerModule],
  templateUrl: './scanner.component.html',
  styleUrl: './scanner.component.scss',
})
export class ScannerComponent {
  allowedFormats = [
    BarcodeFormat.EAN_13,
    BarcodeFormat.EAN_8,
    BarcodeFormat.CODE_128,
    BarcodeFormat.UPC_A,
  ];

  sessionId = input.required<string>();
  currentShareCode = signal<string | null>(null);

  backendService = inject(BackendService);

  availableDevices: MediaDeviceInfo[] = [];
  currentDevice: MediaDeviceInfo | undefined = undefined;
  hasDevices: boolean = false;
  hasPermission: boolean | null = null;

  scannedCodes: string[] = [];
  lastScannedCode: string = '';

  ngOnInit() {
    this.generateShareCode();
  }

  generateShareCode() {
    this.backendService.generateShareCode(this.sessionId()).subscribe({
      next: (result) => {
        this.currentShareCode.set(result.code);
        setTimeout(() => this.generateShareCode(), 300000);
      },
    });
  }

  // Wywoływane, gdy Zxing po starcie wykryje dostępne obiektywy
  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.availableDevices = devices;
    this.hasDevices = Boolean(devices && devices.length > 0);

    // Opcjonalnie: Jeśli chcemy wymusić domyślnie ostatnią kamerę z tyłu
    // (czasami główny obiektyw ładuje się na końcu listy)
    if (this.hasDevices) {
      // this.currentDevice = devices[devices.length - 1];
    }
  }

  // Odpowiedź na to, czy user zezwolił na kamerę w przeglądarce
  onHasPermission(has: boolean) {
    this.hasPermission = has;
  }

  // Kiedy użytkownik zmieni kamerę w SelectBoxie
  onDeviceSelectChange(event: any) {
    const deviceId = event.target.value;
    if (deviceId === '') {
      this.currentDevice = undefined;
    } else {
      this.currentDevice = this.availableDevices.find((d) => d.deviceId === deviceId);
    }
  }

  onCodeResult(resultString: string) {
    if (resultString !== this.lastScannedCode) {
      this.lastScannedCode = resultString;
      this.scannedCodes.unshift(resultString);

      this.provideFeedback();

      this.backendService.notifyDraftBookResult(this.sessionId(), resultString).subscribe({
        next: (result) => {
          console.log(result.status);
        },
      });

      setTimeout(() => {
        this.lastScannedCode = '';
      }, 2000);
    }
  }

  private provideFeedback() {
    if (window.navigator.vibrate) {
      window.navigator.vibrate(50);
    }

    try {
      const context = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = context.createOscillator();
      const gainNode = context.createGain();

      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(800, context.currentTime);

      gainNode.gain.setValueAtTime(0.1, context.currentTime);

      oscillator.connect(gainNode);
      gainNode.connect(context.destination);

      oscillator.start();
      setTimeout(() => {
        oscillator.stop();
      }, 100);
    } catch (e) {
      console.log('Audio API not supported');
    }
  }
}
