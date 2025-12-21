import { Component, input, computed } from '@angular/core';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideFlame } from '@ng-icons/lucide';

/** Number of days to display in the streak visualization */
const STREAK_VISUALIZATION_DAYS = 14;

@Component({
  selector: 'app-streak-bar',
  standalone: true,
  imports: [...HlmCardImports, ...HlmBadgeImports, ...HlmIconImports],
  providers: [provideIcons({ lucideFlame })],
  template: `
    <section hlmCard class="h-full gap-0 p-5">
      <header hlmCardHeader class="mb-3 flex items-start justify-between p-0">
        <div class="flex items-center gap-2">
          <!-- Flame icon -->
          <div
            class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand/10"
          >
            <ng-icon hlm name="lucideFlame" size="sm" class="text-brand" />
          </div>
          <span class="text-sm text-muted-foreground">Current Streak</span>
        </div>

        <!-- Best streak badge -->
        <span
          hlmBadge
          variant="outline"
          class="rounded-full bg-brand/10 text-brand border-brand/20"
        >
          Best: {{ longestStreakValue() }}
        </span>
      </header>

      <div hlmCardContent class="p-0">
        <!-- Value -->
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold text-foreground">{{ currentStreakValue() }}</span>
          <span class="text-sm text-muted-foreground">days</span>
        </div>

        <!-- Streak visualization -->
        <div class="mt-3 flex gap-0.5">
          @for (day of streakDays(); track day.index) {
            <div
              class="h-6 w-2 rounded-sm transition-colors"
              [class.bg-brand]="day.active && day.isToday"
              [class.bg-brand/80]="day.active && !day.isToday"
              [class.bg-muted]="!day.active"
              [title]="day.active ? 'Read' : 'No activity'"
            ></div>
          }
        </div>
      </div>
    </section>
  `,
})
export class StreakBarComponent {
  readonly current = input<number | null>();
  readonly longest = input<number | null>();

  /** Normalized current streak value with null handling */
  readonly currentStreakValue = computed(() => this.current() ?? 0);

  /** Normalized longest streak value with null handling */
  readonly longestStreakValue = computed(() => this.longest() ?? 0);

  readonly streakDays = computed(() => {
    const currentStreak = this.currentStreakValue();
    const days: Array<{ index: number; active: boolean; isToday: boolean }> = [];

    for (let i = 0; i < STREAK_VISUALIZATION_DAYS; i++) {
      // Days are displayed from oldest (left) to newest (right)
      // Today is the rightmost bar
      const daysAgo = STREAK_VISUALIZATION_DAYS - 1 - i;
      const isWithinStreak = daysAgo < currentStreak;
      const isToday = daysAgo === 0;

      days.push({
        index: i,
        active: isWithinStreak,
        isToday,
      });
    }

    return days;
  });
}
