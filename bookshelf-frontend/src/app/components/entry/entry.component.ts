import {
  Component,
  computed,
  ElementRef,
  inject,
  QueryList,
  signal,
  ViewChildren,
} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BackendService } from '../../services/backend.service';
import { Router } from '@angular/router';
import { ToastService } from '../../services/toast.service';
import { RecentSessionsService } from '../../services/recent-session.service';
import { ScreenService } from '../../services/screen.service';

@Component({
  selector: 'app-entry',
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './entry.component.html',
  styleUrl: './entry.component.scss',
})
export class EntryComponent {
  @ViewChildren('codeInput') inputs!: QueryList<ElementRef<HTMLInputElement>>;

  private scannerService = inject(BackendService);
  screenService = inject(ScreenService);
  router = inject(Router);
  toasts = inject(ToastService);
  recentSessionsService = inject(RecentSessionsService);

  shareCodeDigits = signal<string[]>(['', '', '', '', '', '']);
  pendingSessionId = signal<string | null>(null);

  shouldPromptMode = computed(() => {
    if (this.screenService.isNarrow()) {
      return false;
    }
    return (
      this.screenService.isMedium() ||
      (this.screenService.isWide() && this.screenService.hasTouch())
    );
  });

  onInput(event: Event, index: number) {
    const input = event.target as HTMLInputElement;
    let value = input.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

    if (value.length > 1) {
      value = value.slice(-1);
    }

    this.shareCodeDigits.update((digits) => {
      digits[index] = value;
      return [...digits];
    });

    input.value = value;

    if (value && index < 5) {
      this.focusInput(index + 1);
    } else if (index === 5) {
      this.joinByShareCode();
    }
  }

  onKeyDown(event: KeyboardEvent, index: number) {
    if (event.key === 'Backspace') {
      if (!this.shareCodeDigits()[index] && index > 0) {
        this.focusInput(index - 1);
      }
    }
  }

  onPaste(event: ClipboardEvent) {
    event.preventDefault();
    const pasteData =
      event.clipboardData
        ?.getData('text')
        .toUpperCase()
        .replace(/[^A-Z0-9]/g, '') || '';
    if (pasteData) {
      this.shareCodeDigits.update((digits) => {
        for (let i = 0; i < 6; i++) {
          digits[i] = pasteData[i] || '';
        }
        return [...digits];
      });
      const nextIndex = Math.min(pasteData.length, 5);
      if (nextIndex < 5) {
        this.focusInput(nextIndex);
      } else {
        this.joinByShareCode();
      }
    }
  }

  focusInput(index: number) {
    const inputsArray = this.inputs.toArray();
    if (inputsArray[index]) {
      inputsArray[index].nativeElement.focus();
    }
  }

  startSession() {
    this.scannerService.createSession().subscribe({
      next: (response) => this.handleSessionTarget(response.id),
      error: () => this.toasts.show('Error creating cataloging session', 'error'),
    });
  }

  joinByShareCode() {
    const shareCode = this.shareCodeDigits().join('');
    if (shareCode) {
      this.scannerService.retrieveSessionIdByShareCode(shareCode).subscribe({
        next: (response) => this.handleSessionTarget(response.sessionId),
        error: () => this.toasts.show('Session number could not be retrieved', 'error'),
      });
    }
  }

  handleRecentSessionClick(sessionId: string) {
    this.handleSessionTarget(sessionId);
  }

  private handleSessionTarget(sessionId: string) {
    if (this.screenService.isNarrow()) {
      this.router.navigate(['/cataloging-session', sessionId, 'scanner']);
    } else if (this.shouldPromptMode()) {
      this.pendingSessionId.set(sessionId);
    } else {
      this.router.navigate(['/cataloging-session', sessionId]);
    }
  }

  navigateToSession(mode: 'scanner' | 'table') {
    const sessionId = this.pendingSessionId();
    if (!sessionId) return;

    if (mode === 'scanner') {
      this.router.navigate(['/cataloging-session', sessionId, 'scanner']);
    } else {
      this.router.navigate(['/cataloging-session', sessionId]);
    }
  }

  cancelChoice() {
    this.pendingSessionId.set(null);
  }
}
