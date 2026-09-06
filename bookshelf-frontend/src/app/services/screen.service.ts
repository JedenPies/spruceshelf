import { Injectable, signal } from '@angular/core';

export type ScreenTier = 'narrow' | 'medium' | 'wide';

@Injectable({
  providedIn: 'root',
})
export class ScreenService {
  private readonly NARROW_BREAKPOINT = 768;
  private readonly WIDE_BREAKPOINT = 1024;

  screenTier = signal<ScreenTier>(this.getTier());
  hasTouch = signal<boolean>(this.detectTouch());

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', () => {
        this.screenTier.set(this.getTier());
      });
    }
  }

  isNarrow(): boolean {
    return this.screenTier() === 'narrow';
  }

  isMedium(): boolean {
    return this.screenTier() === 'medium';
  }

  isWide(): boolean {
    return this.screenTier() === 'wide';
  }

  private getTier(): ScreenTier {
    if (typeof window === 'undefined') return 'wide';
    const width = window.innerWidth;
    if (width < this.NARROW_BREAKPOINT) return 'narrow';
    if (width <= this.WIDE_BREAKPOINT) return 'medium';
    return 'wide';
  }

  private detectTouch(): boolean {
    if (typeof window === 'undefined' || typeof navigator === 'undefined') {
      return false;
    }
    const hasTouchPoints = navigator.maxTouchPoints > 0;
    const isCoarse = window.matchMedia?.('(pointer: coarse)').matches ?? false;
    const isMobileUa = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

    return hasTouchPoints || isCoarse || isMobileUa;
  }
}
