import { Component, input, output, computed } from '@angular/core';
import { SyncState } from '../../../core/models/sync.models';

@Component({
  selector: 'app-sync-footer',
  standalone: true,
  template: `
    <div class="border-t border-border bg-card px-6 py-4">
      @switch (footerType()) {
        @case ('running') {
          <div class="flex items-center justify-between">
            <div class="text-sm text-muted-foreground">
              Started <span class="font-medium">{{ elapsedTime() }}</span>
            </div>
            <button
              type="button"
              class="rounded-lg bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80"
              (click)="cancelClick.emit()"
            >
              Cancel Sync
            </button>
          </div>
        }
        @case ('completed') {
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30"
              >
                <svg
                  class="h-5 w-5 text-emerald-600 dark:text-emerald-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </div>
              <div>
                <p class="text-sm font-medium">Sync completed</p>
                <p class="text-xs text-muted-foreground">
                  {{ state().phaseCounts.documents }} docs,
                  {{ state().phaseCounts.highlights }} highlights,
                  {{ state().phaseCounts.notes }} notes
                </p>
              </div>
            </div>
            <button
              type="button"
              class="rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-white hover:bg-amber-600"
              (click)="doneClick.emit()"
            >
              Done
            </button>
          </div>
        }
        @case ('failed') {
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/30"
              >
                <svg
                  class="h-5 w-5 text-destructive"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </div>
              <div>
                <p class="text-sm font-medium text-destructive">Sync failed</p>
                <p class="text-xs text-muted-foreground">Check error above</p>
              </div>
            </div>
            <button
              type="button"
              class="rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-white hover:bg-amber-600"
              (click)="retryClick.emit()"
            >
              Retry Sync
            </button>
          </div>
        }
      }
    </div>
  `,
})
export class SyncFooterComponent {
  readonly state = input.required<SyncState>();

  readonly cancelClick = output<void>();
  readonly doneClick = output<void>();
  readonly retryClick = output<void>();

  readonly footerType = computed(() => {
    const status = this.state().status;
    if (status === 'completed') return 'completed';
    if (status === 'failed') return 'failed';
    return 'running';
  });

  readonly elapsedTime = computed(() => {
    const startedAt = this.state().startedAt;
    if (!startedAt) return '0s';

    const elapsed = Math.floor((Date.now() - startedAt.getTime()) / 1000);
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;

    if (minutes === 0) return `${seconds}s`;
    return `${minutes}m ${seconds}s`;
  });
}
