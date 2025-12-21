import { Component, input, effect, signal, OnDestroy } from '@angular/core';
import { RateLimitInfo } from '../../../core/models/sync.models';
import { HlmAlertImports } from '@spartan-ng/helm/alert';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideClock } from '@ng-icons/lucide';

@Component({
  selector: 'app-rate-limit-banner',
  standalone: true,
  imports: [...HlmAlertImports, ...HlmIconImports],
  providers: [provideIcons({ lucideClock })],
  template: `
    @if (rateLimit()) {
      <div
        hlmAlert
        class="rounded-none border-x-0 border-t-0 border-warning/20 bg-warning/5"
      >
        <ng-icon hlmAlertIcon name="lucideClock" class="text-warning" />
        <h5 hlmAlertTitle class="text-warning-foreground">Rate limit reached</h5>
        <p hlmAlertDescription class="text-warning">
          Retrying in
          <span class="font-mono font-bold">{{ countdown() }}</span
          >s (attempt {{ rateLimit()!.attempt }}/{{ rateLimit()!.maxAttempts }})
        </p>
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
