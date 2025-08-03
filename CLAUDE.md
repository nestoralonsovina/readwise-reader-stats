# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Readwise Analytics Dashboard - A comprehensive reading analytics application that syncs with the Readwise Reader API to provide insights into reading habits, progress tracking, and content consumption patterns.

## Architecture

### Tech Stack
- **Backend**: Supabase (PostgreSQL + Edge Functions) with TimescaleDB extension
- **Frontend**: React + TypeScript + Vite with TailwindCSS
- **UI Components**: Neobrutalism components from https://www.neobrutalism.dev/ (NOT shadcn/ui)
- **Authentication**: Supabase Auth
- **API Integration**: Readwise Reader API via Edge Functions

### Project Structure
```
├── frontend/           # React TypeScript frontend
│   ├── src/
│   │   ├── components/ui/  # shadcn/ui components
│   │   └── lib/           # Utility libraries
│   ├── package.json       # Frontend dependencies
│   └── vite.config.ts     # Vite configuration
├── supabase/              # Supabase backend
│   ├── functions/         # Edge Functions (Deno/TypeScript)
│   │   ├── sync-scheduler/    # Main sync orchestration
│   │   └── shared/           # Shared utilities
│   ├── migrations/        # Database schema migrations
│   └── config.toml        # Supabase configuration
└── package.json           # Root dependencies (Supabase CLI)
```

### Key Components

**Edge Functions** (`supabase/functions/`):
- `sync-scheduler/`: Main function for orchestrating Readwise API sync
- `shared/`: Reusable TypeScript modules for Readwise integration
  - `readwise-types.ts`: Type definitions for Readwise API
  - `readwise-base-client.ts`: Base API client
  - `readwise-service.ts`: Service layer for data operations

**Database Schema** (`supabase/migrations/`):
- `documents`: Core document storage with reading progress
- `reading_events`: Time-series data for reading analytics (TimescaleDB)
- `sync_queue`: Background job queue for API synchronization
- `sync_status`: User-level sync state tracking

**Frontend** (`frontend/src/`):
- React + TypeScript SPA
- TailwindCSS with Neobrutalism components
- Path aliases: `@/*` maps to `src/*`

## Development Commands

### Frontend Development
```bash
cd frontend
npm run dev          # Start Vite dev server
npm run build        # Build for production (runs tsc -b && vite build)
npm run lint         # Run ESLint
npm run preview      # Preview production build
```

### Supabase Development
```bash
supabase start       # Start local Supabase stack
supabase stop        # Stop local services
supabase status      # Check service status
supabase functions deploy sync-scheduler  # Deploy specific function
```

### Testing and Quality
- **Linting**: ESLint configured with TypeScript, React Hooks, and React Refresh rules
- **Type Checking**: TypeScript with strict configuration
- **Build**: Always run `tsc -b` before Vite build to catch type errors

## Configuration Details

### TypeScript Configuration
- Composite project structure with separate configs for app and node
- Path mapping: `@/*` resolves to `src/*`
- Strict type checking enabled

### ESLint Configuration (`frontend/eslint.config.js`)
- TypeScript ESLint with recommended rules
- React Hooks plugin for proper hook usage
- React Refresh plugin for Vite HMR

### Supabase Configuration
- Local development on port 54321 (API), 54322 (DB), 54323 (Studio)
- Edge Runtime enabled with Deno v1
- Auth configured for localhost development
- Functions use JWT verification

### UI Component System
- **MANDATORY**: Neobrutalism components from https://www.neobrutalism.dev/
- TailwindCSS with CSS variables
- Lucide React for icons
- Component aliases configured for easy imports
- **NEVER use shadcn/ui** - only Neobrutalism components

## API Integration Patterns

### Readwise API
- Rate limited to 20 requests/minute
- Cursor-based pagination
- Document sync with incremental updates
- Error handling with exponential backoff

### Database Patterns
- TimescaleDB for time-series reading events
- UUID primary keys with foreign key relationships
- User-scoped data with RLS (Row Level Security)
- Background job processing via sync queue

## Environment Requirements

### Required Environment Variables
```bash
# Supabase
NEXT_PUBLIC_SUPABASE_URL=your-project-url
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# Readwise Integration
READWISE_ACCESS_TOKEN=your-readwise-token
```

### Prerequisites
- Node.js 18+
- Supabase CLI (`npm install -g supabase`)
- Readwise Reader API access
- PostgreSQL with TimescaleDB (handled by Supabase)

## Common Development Workflows

1. **Adding New UI Components**: Use Neobrutalism components from https://www.neobrutalism.dev/ - copy components to `frontend/src/components/ui/`
2. **Database Changes**: Create new migration with `supabase migration new [name]`
3. **Edge Functions**: Add to `supabase/functions/` with proper Deno imports
4. **API Integration**: Extend shared Readwise service modules for consistency
5. **Frontend Development**: Use path aliases and follow React/TypeScript best practices

## Deployment Notes

- Edge Functions deployed individually via Supabase CLI
- Frontend can be deployed to any static hosting (Vercel, Netlify, etc.)
- Database migrations run automatically on Supabase platform
- Cron jobs configured via SQL for scheduled sync operations