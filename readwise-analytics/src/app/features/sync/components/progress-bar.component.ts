import { Component, input, computed } from '@angular/core';

@Component({
  selector: 'app-progress-bar',
  standalone: true,
  template: `
    <div class="mt-5">
      <div class="mb-1.5 flex items-center justify-between">
        <span class="text-xs text-muted-foreground">Overall Progress</span>
        <span class="text-xs font-medium">{{ percent() }}%</span>
      </div>
      <div class="h-2 overflow-hidden rounded-full bg-muted">
        <div
          class="h-full rounded-full transition-all duration-300"
          [class]="barClasses()"
          [style.width.%]="percent()"
        ></div>
      </div>
    </div>
  `,
  styles: [
    `
      @keyframes progress-animation {
        0% {
          background-position: 1rem 0;
        }
        100% {
          background-position: 0 0;
        }
      }

      .progress-animated {
        background-image: linear-gradient(
          -45deg,
          rgba(255, 255, 255, 0.15) 25%,
          transparent 25%,
          transparent 50%,
          rgba(255, 255, 255, 0.15) 50%,
          rgba(255, 255, 255, 0.15) 75%,
          transparent 75%,
          transparent
        );
        background-size: 1rem 1rem;
        animation: progress-animation 1s linear infinite;
      }
    `,
  ],
})
export class ProgressBarComponent {
  readonly percent = input.required<number>();
  readonly isAnimated = input(false);
  readonly isError = input(false);

  readonly barClasses = computed(() => {
    const classes: string[] = [];

    if (this.isError()) {
      classes.push('bg-destructive');
    } else {
      classes.push('bg-brand');
    }

    if (this.isAnimated()) {
      classes.push('progress-animated');
    }

    return classes.join(' ');
  });
}
