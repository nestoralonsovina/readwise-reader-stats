import { Component, input, output } from '@angular/core';
import { Period } from '../../../core/models/api.models';

@Component({
  selector: 'app-period-toggle',
  standalone: true,
  template: `
    <div
      class="inline-flex rounded-lg border border-border bg-muted p-1"
    >
      @for (option of options; track option.value) {
        <button
          type="button"
          class="rounded-md px-3 py-1.5 text-sm font-medium transition-colors"
          [class.bg-card]="period() === option.value"
          [class.text-foreground]="period() === option.value"
          [class.shadow-sm]="period() === option.value"
          [class.text-muted-foreground]="period() !== option.value"
          [class.hover:text-foreground]="period() !== option.value"
          (click)="selectPeriod(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>
  `,
})
export class PeriodToggleComponent {
  readonly period = input.required<Period>();
  readonly periodChange = output<Period>();

  readonly options: ReadonlyArray<{ label: string; value: Period }> = [
    { label: 'Week', value: 7 },
    { label: 'Month', value: 30 },
    { label: 'Year', value: 365 },
  ];

  selectPeriod(value: Period): void {
    this.periodChange.emit(value);
  }
}
