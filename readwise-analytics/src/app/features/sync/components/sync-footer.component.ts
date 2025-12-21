import { Component, input, output, computed } from '@angular/core';
import { SyncState } from '../../../core/models/sync.models';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { BrnSheetClose } from '@spartan-ng/brain/sheet';
import { provideIcons } from '@ng-icons/core';
import { lucideCheck, lucideX, lucideRefreshCw } from '@ng-icons/lucide';

type FooterType = 'idle' | 'running' | 'completed' | 'failed';

@Component({
  selector: 'app-sync-footer',
  standalone: true,
  imports: [...HlmButtonImports, ...HlmIconImports, BrnSheetClose],
  providers: [provideIcons({ lucideCheck, lucideX, lucideRefreshCw })],
  template: `
    <div class="px-6 py-4">
      @switch (footerType()) {
        @case ('idle') {
          <div class="flex items-center justify-between">
            <div class="text-sm text-muted-foreground">
              Ready to sync your Readwise library
            </div>
            <button
              hlmBtn
              class="bg-brand text-brand-foreground hover:bg-brand/90"
              (click)="startClick.emit()"
            >
              <ng-icon hlm name="lucideRefreshCw" size="sm" />
              Start Sync
            </button>
          </div>
        }
        @case ('running') {
          <div class="flex items-center justify-between">
            <div class="text-sm text-muted-foreground">
              Started <span class="font-medium">{{ elapsedTime() }}</span>
            </div>
            <button hlmBtn variant="secondary" (click)="cancelClick.emit()">
              Cancel Sync
            </button>
          </div>
        }
        @case ('completed') {
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full bg-success/10"
              >
                <ng-icon hlm name="lucideCheck" class="text-success" />
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
              hlmBtn
              brnSheetClose
              class="bg-brand text-brand-foreground hover:bg-brand/90"
            >
              Done
            </button>
          </div>
        }
        @case ('failed') {
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10"
              >
                <ng-icon hlm name="lucideX" class="text-destructive" />
              </div>
              <div>
                <p class="text-sm font-medium text-destructive">Sync failed</p>
                <p class="text-xs text-muted-foreground">Check error above</p>
              </div>
            </div>
            <button
              hlmBtn
              class="bg-brand text-brand-foreground hover:bg-brand/90"
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

  readonly startClick = output<void>();
  readonly cancelClick = output<void>();
  readonly retryClick = output<void>();

  readonly footerType = computed<FooterType>(() => {
    const status = this.state().status;
    if (status === 'completed') return 'completed';
    if (status === 'failed' || status === 'cancelled') return 'failed';
    if (status === 'running' || status === 'rate_limited') return 'running';
    return 'idle';
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
