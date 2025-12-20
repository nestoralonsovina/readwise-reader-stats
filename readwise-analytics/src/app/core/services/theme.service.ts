import { Injectable, signal, effect, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  readonly isDark = signal(this.getInitialTheme());

  constructor() {
    if (this.isBrowser) {
      effect(() => {
        const dark = this.isDark();
        document.documentElement.classList.toggle('dark', dark);
        localStorage.setItem('theme', dark ? 'dark' : 'light');
      });
    }
  }

  toggle(): void {
    this.isDark.update((v) => !v);
  }

  private getInitialTheme(): boolean {
    if (!this.isBrowser) {
      return false;
    }

    const stored = localStorage.getItem('theme');
    if (stored) {
      return stored === 'dark';
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
