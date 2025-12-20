import { Component, computed, input, output } from '@angular/core';
import { Period } from '../../../core/models/api.models';
import { HlmToggleGroupImports } from '@spartan-ng/helm/toggle-group';

@Component({
  selector: 'app-period-toggle',
  standalone: true,
  imports: [...HlmToggleGroupImports],
  template: `
    <div
      hlmToggleGroup
      type="single"
      [value]="periodAsString()"
      (valueChange)="onValueChange($event)"
      variant="outline"
      class="bg-muted p-1 rounded-lg border border-border"
    >
      @for (option of options; track option.value) {
        <button
          hlmToggleGroupItem
          [value]="option.value.toString()"
          class="data-[state=on]:bg-card data-[state=on]:shadow-sm"
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

  readonly periodAsString = computed(() => this.period().toString());

  readonly options: ReadonlyArray<{ label: string; value: Period }> = [
    { label: 'Week', value: 7 },
    { label: 'Month', value: 30 },
    { label: 'Year', value: 365 },
  ];

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onValueChange(value: unknown): void {
    if (typeof value !== 'string' || !value) return;
    const period = Number(value) as Period;
    this.periodChange.emit(period);
  }
}
