import { Component, computed, input } from '@angular/core';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideClock } from '@ng-icons/lucide';

@Component({
  selector: 'app-coming-soon-badge',
  standalone: true,
  imports: [...HlmBadgeImports, ...HlmIconImports],
  providers: [provideIcons({ lucideClock })],
  template: `
    <span
      hlmBadge
      [variant]="badgeVariant()"
      [class]="variantClass()"
      [title]="tooltip()"
    >
      @if (showIcon()) {
        <ng-icon name="lucideClock" size="xs" />
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

  readonly badgeVariant = computed(() => (this.variant() === 'muted' ? 'secondary' : 'outline'));

  readonly variantClass = computed(() =>
    this.variant() === 'accent'
      ? 'rounded-full bg-amber-100 text-amber-700 border-amber-200 dark:bg-amber-900/30 dark:text-amber-400 dark:border-amber-800'
      : 'rounded-full'
  );
}
