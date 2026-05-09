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
│   │   └── shell.component.ts          # Main layout with sidebar
│   ├── models/
│   │   ├── api.models.ts              # Dashboard, drill-down, document detail types
│   │   └── sync.models.ts             # SSE event types and sync state
│   └── services/
│       ├── analytics.service.ts        # GET /api/analytics/*
│       ├── chart-colors.service.ts     # Chart color palette (light/dark)
│       ├── document.service.ts         # GET /api/documents/:id
│       ├── drill-down.service.ts       # GET /api/analytics/drill-down/*
│       ├── sync.service.ts            # POST /sync, SSE stream, cancel
│       └── theme.service.ts           # Dark mode toggle
├── features/
│   ├── dashboard/
│   │   ├── dashboard.component.ts      # Container with signal state
│   │   └── components/
│   │       ├── dashboard-header/
│   │       ├── dashboard-footer/
│   │       ├── reading-activity-chart/  # ApexCharts area+line
│   │       ├── pipeline-card/           # Progress bars
│   │       ├── highlights-card/         # Donut chart
│   │       └── most-highlighted/        # Article list
│   ├── library/
│   │   ├── document-detail.component.ts # Document detail page
│   │   └── components/
│   │       ├── highlight-list.component.ts
│   │       └── highlight-item.component.ts
│   └── sync/
│       ├── sync-panel.component.ts      # Spartan Sheet slide-over
│       └── components/
│           ├── phase-stepper.component.ts
│           ├── activity-log.component.ts
│           ├── rate-limit-banner.component.ts
│           └── sync-footer.component.ts
└── shared/
    ├── components/
    │   ├── sidebar/                     # Navigation with route links
    │   ├── kpi-card/                    # Metric card with sparkline
    │   ├── streak-bar/                  # 14-day streak visualization
    │   ├── period-toggle/               # Week/Month/Year selector
    │   ├── reading-timeline/            # Document reading progress timeline
    │   ├── document-row/                # Row for drill-down lists
    │   ├── stat-drill-down-sheet/       # Spartan Sheet slide-over
    │   └── coming-soon-badge/           # Feature availability indicator
    └── pipes/
        └── format-number.pipe.ts        # 15400 → "15.4K"
```

## Services

| Service | Endpoints |
|---------|-----------|
| `AnalyticsService` | `GET /api/analytics/dashboard`, `reading/stats`, `reading/streak`, `reading/peak-hours`, `pipeline`, `highlights` |
| `DrillDownService` | `GET /api/analytics/drill-down/words-read`, `drill-down/completed`, `drill-down/backlog` |
| `DocumentService` | `GET /api/documents/:id` |
| `SyncService` | `POST /sync`, SSE `/sync/:id/stream`, `GET /sync/active`, `DELETE /sync/:id` (cancel) |
| `ChartColorsService` | Local palette generation (no API) |
| `ThemeService` | Local dark mode (no API) |

## SSE Pattern

`SyncService` connects to `/sync/{id}/stream` and dispatches typed events:

```typescript
const eventMap: Record<SyncEventType, (data: any) => void> = {
  phase_started: (d) => this.onPhaseStarted(d),
  progress: (d) => this.onProgress(d),
  phase_completed: (d) => this.onPhaseCompleted(d),
  rate_limited: (d) => this.onRateLimited(d),
  completed: (d) => this.onCompleted(d),
  error: (d) => this.onError(d),
  cancelled: (d) => this.onCancelled(d),
};
```

State managed with Angular Signals. Reconnection handled via `lastEventId`.

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