import {
  Component,
  input,
  signal,
  effect,
  ElementRef,
  viewChild,
} from '@angular/core';
import { LogEntry, LogEntryType } from '../../../core/models/sync.models';

@Component({
  selector: 'app-activity-log',
  standalone: true,
  template: `
    <div class="flex-1 overflow-y-auto">
      <div
        class="sticky top-0 flex items-center justify-between border-b border-border/50 bg-card px-6 py-3"
      >
        <span class="text-sm font-medium">Activity Log</span>
        <label
          class="flex cursor-pointer items-center gap-2 text-xs text-muted-foreground"
        >
          <input
            type="checkbox"
            class="h-3.5 w-3.5 rounded border-border text-brand focus:ring-brand"
            [checked]="showVerbose()"
            (change)="showVerbose.set(!showVerbose())"
          />
          Verbose
        </label>
      </div>

      <div #logContainer class="space-y-3 px-6 py-4">
        @for (entry of filteredLogs(); track entry.id) {
          <div class="log-entry flex items-start gap-3">
            <span
              class="w-16 flex-shrink-0 pt-0.5 font-mono text-xs text-muted-foreground"
            >
              {{ entry.timestamp }}
            </span>
            <div class="flex-shrink-0 pt-0.5">
              @switch (entry.type) {
                @case ('success') {
                  <svg
                    class="h-4 w-4 text-success"
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
                }
                @case ('complete') {
                  <svg
                    class="h-4 w-4 text-success"
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
                }
                @case ('warning') {
                  <svg
                    class="h-4 w-4 text-warning"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                    />
                  </svg>
                }
                @case ('error') {
                  <svg
                    class="h-4 w-4 text-destructive"
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
                }
                @case ('phase') {
                  <svg
                    class="h-4 w-4 text-chart-1"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M13 10V3L4 14h7v7l9-11h-7z"
                    />
                  </svg>
                }
                @case ('progress') {
                  <svg
                    class="h-4 w-4 text-muted-foreground"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9 5l7 7-7 7"
                    />
                  </svg>
                }
                @default {
                  <svg
                    class="h-4 w-4 text-muted-foreground"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                }
              }
            </div>
            <span class="text-sm" [class]="getEntryClasses(entry.type)">
              {{ entry.message }}
            </span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      @keyframes slideIn {
        from {
          opacity: 0;
          transform: translateY(-8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .log-entry {
        animation: slideIn 0.2s ease-out;
      }
    `,
  ],
})
export class ActivityLogComponent {
  readonly logs = input.required<readonly LogEntry[]>();

  readonly showVerbose = signal(false);
  readonly logContainer = viewChild<ElementRef<HTMLDivElement>>('logContainer');

  constructor() {
    // Auto-scroll to bottom when new logs arrive
    effect(() => {
      const logs = this.logs();
      const container = this.logContainer()?.nativeElement;
      if (logs.length > 0 && container) {
        setTimeout(() => {
          container.scrollTop = container.scrollHeight;
        }, 50);
      }
    });
  }

  filteredLogs(): readonly LogEntry[] {
    if (this.showVerbose()) {
      return this.logs();
    }
    return this.logs().filter((log) => log.type !== 'progress');
  }

  getEntryClasses(type: LogEntryType): string {
    switch (type) {
      case 'success':
        return 'text-success';
      case 'complete':
        return 'font-medium text-success';
      case 'warning':
        return 'text-warning';
      case 'error':
        return 'text-destructive';
      case 'phase':
        return 'font-medium text-chart-1';
      case 'progress':
        return 'text-muted-foreground';
      default:
        return 'text-foreground';
    }
  }
}
