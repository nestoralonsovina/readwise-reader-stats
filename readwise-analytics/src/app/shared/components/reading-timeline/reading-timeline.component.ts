import { Component, input, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import {
  lucideBookmarkPlus,
  lucideBookOpen,
  lucideEye,
  lucideCheckCircle,
} from '@ng-icons/lucide';

interface TimelineEvent {
  readonly icon: string;
  readonly label: string;
  readonly date: string | null;
  readonly isCompleted: boolean;
}

@Component({
  selector: 'app-reading-timeline',
  standalone: true,
  imports: [DatePipe, ...HlmIconImports],
  providers: [
    provideIcons({
      lucideBookmarkPlus,
      lucideBookOpen,
      lucideEye,
      lucideCheckCircle,
    }),
  ],
  template: `
    <section class="rounded-lg border border-border bg-card p-6">
      <h3 class="mb-4 text-sm font-medium text-muted-foreground">Reading Timeline</h3>

      <div class="relative flex items-center justify-between">
        <!-- Connecting line -->
        <div
          class="absolute left-0 right-0 top-1/2 h-0.5 -translate-y-1/2 bg-border"
        ></div>

        @for (event of timelineEvents(); track event.label) {
          <div class="relative z-10 flex flex-col items-center">
            <!-- Icon circle -->
            <div
              class="flex h-10 w-10 items-center justify-center rounded-full border-2"
              [class]="
                event.date
                  ? 'border-chart-1 bg-chart-1/10 text-chart-1'
                  : 'border-border bg-muted text-muted-foreground'
              "
            >
              <ng-icon hlm [name]="event.icon" size="sm" />
            </div>

            <!-- Label -->
            <span class="mt-2 text-xs font-medium text-foreground">
              {{ event.label }}
            </span>

            <!-- Date -->
            <span class="text-xs text-muted-foreground">
              @if (event.date) {
                {{ event.date | date: 'MMM d' }}
              } @else {
                —
              }
            </span>
          </div>
        }
      </div>
    </section>
  `,
})
export class ReadingTimelineComponent {
  readonly savedAt = input<string | null>(null);
  readonly firstOpenedAt = input<string | null>(null);
  readonly lastOpenedAt = input<string | null>(null);
  readonly completedAt = input<string | null>(null);

  readonly timelineEvents = computed((): readonly TimelineEvent[] => [
    {
      icon: 'lucideBookmarkPlus',
      label: 'Saved',
      date: this.savedAt(),
      isCompleted: this.savedAt() !== null,
    },
    {
      icon: 'lucideBookOpen',
      label: 'First Opened',
      date: this.firstOpenedAt(),
      isCompleted: this.firstOpenedAt() !== null,
    },
    {
      icon: 'lucideEye',
      label: 'Last Read',
      date: this.lastOpenedAt(),
      isCompleted: this.lastOpenedAt() !== null,
    },
    {
      icon: 'lucideCheckCircle',
      label: 'Completed',
      date: this.completedAt(),
      isCompleted: this.completedAt() !== null,
    },
  ]);
}
