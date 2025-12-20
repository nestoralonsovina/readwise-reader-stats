import { Component, input, effect, signal, OnDestroy } from '@angular/core';
import { RateLimitInfo } from '../../../core/models/sync.models';

@Component({
  selector: 'app-rate-limit-banner',
  standalone: true,
  template: `
    @if (rateLimit()) {
      <div
        class="border-b border-amber-200 bg-amber-50 px-6 py-4 dark:border-amber-800 dark:bg-amber-900/20"
      >
        <div class="flex items-start gap-3">
          <div
            class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/40"
          >
            <svg
              class="h-4 w-4 text-amber-600 dark:text-amber-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
          </div>
          <div class="flex-1">
            <p class="text-sm font-medium text-amber-800 dark:text-amber-200">
              Rate limit reached
            </p>
            <p class="text-sm text-amber-600 dark:text-amber-400">
              Retrying in
              <span class="font-mono font-bold">{{ countdown() }}</span
              >s (attempt {{ rateLimit()!.attempt }}/{{ rateLimit()!.maxAttempts }})
            </p>
          </div>
        </div>
      </div>
    }
  `,
})
export class RateLimitBannerComponent implements OnDestroy {
  readonly rateLimit = input<RateLimitInfo | null>(null);

  readonly countdown = signal(0);
  private intervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      const info = this.rateLimit();
      this.clearInterval();

      if (info) {
        this.countdown.set(info.retryAfter);
        this.intervalId = setInterval(() => {
          this.countdown.update((c) => Math.max(0, c - 1));
        }, 1000);
      }
    });
  }

  ngOnDestroy(): void {
    this.clearInterval();
  }

  private clearInterval(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }
}
