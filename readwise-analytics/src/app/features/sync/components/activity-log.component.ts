import {
  Component,
  input,
  signal,
  effect,
  ElementRef,
  viewChild,
} from '@angular/core';
import { LogEntry, LogEntryType } from '../../../core/models/sync.models';
import { HlmCheckboxImports } from '@spartan-ng/helm/checkbox';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { provideIcons } from '@ng-icons/core';
import {
  lucideCheck,
  lucideAlertTriangle,
  lucideX,
  lucideZap,
  lucideChevronRight,
  lucideInfo,
} from '@ng-icons/lucide';

@Component({
  selector: 'app-activity-log',
  standalone: true,
  imports: [...HlmCheckboxImports, ...HlmIconImports, ...HlmLabelImports],
  providers: [
    provideIcons({
      lucideCheck,
      lucideAlertTriangle,
      lucideX,
      lucideZap,
      lucideChevronRight,
      lucideInfo,
    }),
  ],
  template: `
    <div class="flex flex-1 flex-col overflow-hidden">
      <div
        class="flex items-center justify-between border-b border-border/50 bg-card px-6 py-3"
      >
        <span class="text-sm font-medium">Activity Log</span>
        <label hlmLabel class="flex cursor-pointer items-center gap-2 text-xs">
          <hlm-checkbox
            class="h-3.5 w-3.5"
            [checked]="showVerbose()"
            (checkedChange)="showVerbose.set($event)"
          />
          Verbose
        </label>
      </div>

      <div class="flex-1 overflow-y-auto">
        <div #logContainer class="space-y-3 px-6 py-4">
          @for (entry of filteredLogs(); track entry.id) {
            <div class="log-entry flex items-start gap-3">
              <span
                class="w-16 flex-shrink-0 pt-0.5 font-mono text-xs text-muted-foreground"
              >
                {{ entry.timestamp }}
              </span>
              <div class="flex-shrink-0 pt-0.5">
                <ng-icon
                  hlm
                  [name]="getIconName(entry.type)"
                  size="sm"
                  [class]="getIconClasses(entry.type)"
                />
              </div>
              <span class="text-sm" [class]="getEntryClasses(entry.type)">
                {{ entry.message }}
              </span>
            </div>
          }
        </div>
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

  getIconName(type: LogEntryType): string {
    switch (type) {
      case 'success':
      case 'complete':
        return 'lucideCheck';
      case 'warning':
        return 'lucideAlertTriangle';
      case 'error':
        return 'lucideX';
      case 'phase':
        return 'lucideZap';
      case 'progress':
        return 'lucideChevronRight';
      default:
        return 'lucideInfo';
    }
  }

  getIconClasses(type: LogEntryType): string {
    switch (type) {
      case 'success':
      case 'complete':
        return 'text-success';
      case 'warning':
        return 'text-warning';
      case 'error':
        return 'text-destructive';
      case 'phase':
        return 'text-chart-1';
      case 'progress':
      default:
        return 'text-muted-foreground';
    }
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
