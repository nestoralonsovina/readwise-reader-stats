# CLAUDE.md

This file provides guidance to Claude Code when working with the frontend codebase.

## Project Overview

Angular 21 dashboard for Readwise Reader Analytics. Displays reading metrics, charts, and highlights from a Spring Boot backend.

## Commands

```bash
bun install              # Install dependencies
bun run start            # Dev server with API proxy (localhost:4200)
bun run build            # Production build
bun run test             # Run Vitest tests
```

## Architecture

### Directory Structure

```
src/app/
├── core/                    # Singleton services and models
│   ├── models/api.models.ts # All API response interfaces
│   └── services/
│       ├── analytics.service.ts  # GET /api/analytics/*
│       ├── sync.service.ts       # POST /sync
│       └── theme.service.ts      # Dark mode toggle
├── shared/                  # Reusable across features
│   ├── components/
│   │   ├── kpi-card/        # Metric card with sparkline
│   │   ├── streak-bar/      # 14-day streak visualization
│   │   └── period-toggle/   # Week/Month/Year selector
│   └── pipes/
│       └── format-number.pipe.ts  # 15400 → "15.4K"
└── features/
    └── dashboard/           # Main dashboard feature
        ├── dashboard.component.ts  # Container with signal state
        └── components/
            ├── dashboard-header/
            ├── reading-activity-chart/  # ApexCharts area+line
            ├── peak-hours-heatmap/      # Pure CSS grid
            ├── pipeline-card/           # Progress bars
            ├── highlights-card/         # Donut chart
            ├── most-highlighted/        # Article list
            └── dashboard-footer/
```

### Key Patterns

**Standalone Components**: All components use `standalone: true`. No NgModules.

**Signal-based State**: Dashboard uses Angular signals for reactive state:
```typescript
readonly period = signal<Period>(30);
readonly data = signal<DashboardData | null>(null);
readonly loading = signal(true);
```

**Computed Values**: Derived state via `computed()`:
```typescript
readonly wordsRead = computed(() => this.data()?.dashboard.summary.wordsRead ?? null);
```

**Template Type Narrowing**: Use `@if (data(); as d)` for null-safe template access.

### API Integration

Services in `core/services/` call backend endpoints. Proxy configured in `proxy.conf.json`:
- `/api/*` → `http://localhost:8080`
- `/sync` → `http://localhost:8080`

Response types match backend DTOs exactly (see `api.models.ts`).

### Styling

- **Tailwind CSS 4**: Utility-first, CSS-based config (no `tailwind.config.js`)
- **Spartan UI**: Component library with `@spartan-ng/brain`
- **Theme Tokens**: CSS variables in `src/styles.css`
- **Dark Mode**: Class strategy (`.dark` on `<html>`), persisted to localStorage

### Charts

Using `ng-apexcharts` wrapper:
```typescript
import { NgApexchartsModule } from 'ng-apexcharts';

@Component({
  imports: [NgApexchartsModule],
  template: `<apx-chart [series]="series()" [chart]="chartOptions" />`
})
```

ApexCharts script loaded globally via `angular.json` scripts array.

## Type Safety

- All API responses have readonly interfaces
- No `any` or `as` type assertions
- Exhaustive switch statements use `never` checks
- Null handling via proper type narrowing, not unsafe casts

## Testing

Vitest configured. Run with `bun run test`.
