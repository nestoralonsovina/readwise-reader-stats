# Frontend - Angular 21

## Commands

```bash
bun install              # Install dependencies
bun run start            # Dev server with API proxy (localhost:4200)
bun run build            # Production build
bun run test             # Run Vitest tests
```

API proxy: `/api/*` and `/sync` → `http://localhost:8080`

## Directory Structure

```
src/app/
├── core/
│   ├── layout/
│   │   └── shell.component.ts      # Main layout with sidebar
│   ├── models/api.models.ts        # All API response interfaces
│   └── services/
│       ├── analytics.service.ts    # GET /api/analytics/*
│       ├── sync.service.ts         # POST /sync
│       └── theme.service.ts        # Dark mode toggle
├── shared/
│   ├── components/
│   │   ├── sidebar/                # Navigation with "Coming Soon" badges
│   │   ├── kpi-card/               # Metric card with sparkline
│   │   ├── streak-bar/             # 14-day streak visualization
│   │   ├── period-toggle/          # Week/Month/Year selector
│   │   └── coming-soon-badge/      # Feature availability indicator
│   └── pipes/
│       └── format-number.pipe.ts   # 15400 → "15.4K"
└── features/
    └── dashboard/
        ├── dashboard.component.ts  # Container with signal state
        └── components/
            ├── dashboard-header/
            ├── reading-activity-chart/   # ApexCharts area+line
            ├── peak-hours-heatmap/       # Pure CSS grid
            ├── pipeline-card/            # Progress bars
            ├── highlights-card/          # Donut chart
            ├── most-highlighted/         # Article list
            └── dashboard-footer/
```

## Key Patterns

**Standalone Components**: All use `standalone: true`. No NgModules.

**Signal-based State**:
```typescript
readonly period = signal<Period>(30);
readonly data = signal<DashboardData | null>(null);
```

**Computed Values**: Derived state via `computed()`.

**Template Type Narrowing**: Use `@if (data(); as d)` for null-safe access.

## Styling

- **Tailwind CSS 4**: CSS-based config (no `tailwind.config.js`)
- **Spartan UI**: Component library (`@spartan-ng/brain`)
- **Dark Mode**: Class strategy (`.dark` on `<html>`), localStorage persisted
- **Theme Tokens**: CSS variables in `src/styles.css`

## Charts

Using `ng-apexcharts`:
```typescript
import { NgApexchartsModule } from 'ng-apexcharts';
```

ApexCharts loaded globally via `angular.json` scripts array.

## Type Safety

- All API responses have readonly interfaces
- No `any` or `as` type assertions
- Exhaustive switch with `never` checks
- Null handling via proper narrowing
