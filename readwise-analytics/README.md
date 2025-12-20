# Readwise Reader Analytics - Frontend

Angular 21 dashboard for visualizing reading analytics from the Readwise Reader API.

## Tech Stack

- **Angular 21** with standalone components
- **Tailwind CSS 4** for styling
- **Spartan UI** component library
- **ApexCharts** via ng-apexcharts for data visualization
- **Bun** as package manager

## Quick Start

```bash
# Install dependencies
bun install

# Start dev server (proxies API to localhost:8080)
bun run start

# Build for production
bun run build

# Run tests
bun run test
```

Open http://localhost:4200 after starting the dev server.

## Project Structure

```
src/app/
├── core/
│   ├── models/           # API response types
│   └── services/         # HTTP services, theme service
├── shared/
│   ├── components/       # Reusable UI components
│   └── pipes/            # Format pipes
└── features/
    └── dashboard/        # Main dashboard feature
        └── components/   # Dashboard-specific components
```

## Features

- **KPI Cards**: Words read, articles completed, streak, backlog
- **Reading Activity Chart**: Area + line combo chart with dual Y-axis
- **Peak Hours Heatmap**: CSS grid visualization of reading patterns
- **Content Pipeline**: Progress bars showing document locations
- **Highlights**: Donut chart with color distribution
- **Most Highlighted**: Top documents by highlight count
- **Dark Mode**: System preference detection + manual toggle
- **Period Selection**: Week / Month / Year views

## API Endpoints

The frontend expects a backend running on `localhost:8080` with these endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /api/analytics/dashboard?days=N` | Summary metrics |
| `GET /api/analytics/reading/stats?granularity=X` | Time-series data |
| `GET /api/analytics/reading/streak` | Current/longest streak |
| `GET /api/analytics/reading/peak-hours?days=N` | Hourly activity |
| `GET /api/analytics/pipeline?days=N` | Document pipeline |
| `GET /api/analytics/highlights?days=N` | Highlight stats |
| `POST /sync` | Trigger data sync |

## Development

### Proxy Configuration

API calls are proxied to the backend via `proxy.conf.json`. Start the backend before running the frontend.

### Adding Components

```bash
# Generate a new component
ng generate component features/dashboard/components/my-component
```

### Styling

Uses Tailwind CSS with Spartan UI design tokens. Theme colors defined in `src/styles.css`:

- Primary accent: Amber (`#f59e0b`)
- Dark mode: Class-based toggle (`.dark` on `<html>`)

## Build

```bash
bun run build
```

Output in `dist/readwise-analytics/`. Bundle includes lazy-loaded ApexCharts module.
