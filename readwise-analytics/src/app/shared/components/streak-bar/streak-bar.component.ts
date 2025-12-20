import { Component, input, computed } from '@angular/core';

/** Number of days to display in the streak visualization */
const STREAK_VISUALIZATION_DAYS = 14;

@Component({
  selector: 'app-streak-bar',
  standalone: true,
  template: `
    <div class="rounded-xl border border-border bg-card p-5 transition-colors">
      <div class="mb-3 flex items-start justify-between">
        <div class="flex items-center gap-2">
          <!-- Flame icon -->
          <div
            class="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-100 dark:bg-amber-900/30"
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
                d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"
              />
            </svg>
          </div>
          <span class="text-sm text-muted-foreground">Current Streak</span>
        </div>

        <!-- Best streak badge -->
        <span
          class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
        >
          Best: {{ longestStreakValue() }}
        </span>
      </div>

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
            [class.bg-amber-500]="day.active && day.isToday"
            [class.bg-amber-400]="day.active && !day.isToday"
            [class.bg-muted]="!day.active"
            [title]="day.active ? 'Read' : 'No activity'"
          ></div>
        }
      </div>
    </div>
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
