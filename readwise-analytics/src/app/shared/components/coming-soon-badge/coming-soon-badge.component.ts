import { Component, input } from '@angular/core';

@Component({
  selector: 'app-coming-soon-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
      [class]="variant() === 'muted'
        ? 'bg-muted text-muted-foreground'
        : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'"
      [title]="tooltip()"
    >
      @if (showIcon()) {
        <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      }
      {{ label() }}
    </span>
  `,
})
export class ComingSoonBadgeComponent {
  readonly label = input<string>('Coming Soon');
  readonly tooltip = input<string>('This feature is planned for a future release');
  readonly variant = input<'accent' | 'muted'>('accent');
  readonly showIcon = input<boolean>(false);
}
