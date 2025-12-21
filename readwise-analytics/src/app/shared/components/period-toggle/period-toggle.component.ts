import { Component, computed, input, output } from '@angular/core';
import { Period, FixedPeriod, isCustomPeriod } from '../../../core/models/api.models';
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
      @if (isCustom()) {
        <button
          hlmToggleGroupItem
          value="custom"
          class="data-[state=on]:bg-card data-[state=on]:shadow-sm"
        >
          Custom
        </button>
      }
    </div>
  `,
})
export class PeriodToggleComponent {
  readonly period = input.required<Period>();
  readonly periodChange = output<FixedPeriod>();

  readonly isCustom = computed(() => isCustomPeriod(this.period()));

  readonly periodAsString = computed(() => {
    const p = this.period();
    return isCustomPeriod(p) ? 'custom' : p.toString();
  });

  readonly options: ReadonlyArray<{ label: string; value: FixedPeriod }> = [
    { label: 'Week', value: 7 },
    { label: 'Month', value: 30 },
    { label: 'Year', value: 365 },
  ];

  onValueChange(value: unknown): void {
    if (typeof value !== 'string' || !value) return;
    if (value === 'custom') {
      // Custom period is display-only; user must select a fixed period to change
      return;
    }
    const period = Number(value) as FixedPeriod;
    this.periodChange.emit(period);
  }
}
