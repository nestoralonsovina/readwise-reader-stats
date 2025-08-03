# Readwise Analytics Dashboard

A comprehensive reading analytics application that syncs with the Readwise Reader API to provide insights into your reading habits, progress tracking, and content consumption patterns.

## 📋 Project Status

### Core Analytics
- [ ] **Reading Progress Tracking**: Monitor completion rates across all your documents
- [ ] **Time-Based Metrics**: Track reading velocity, session duration, and reading streaks
- [ ] **Content Analysis**: Breakdown by category, source type, and content length preferences
- [ ] **Highlighting Insights**: Analyze highlight density and knowledge retention patterns
- [ ] **Reading Patterns**: Discover your peak reading hours and preferred content types

### Visualizations
- [ ] Reading streak calendar (GitHub-style contribution graph)
- [ ] Progress ring charts by content type
- [ ] Reading velocity trends with moving averages
- [ ] Interactive category distribution sunburst charts
- [ ] Time-based heatmaps for reading patterns
- [ ] Author network graphs based on shared tags

### Smart Sync System
- [ ] Respects Readwise API rate limits (20 requests/minute)
- [ ] Incremental syncing to minimize API calls
- [ ] Automatic retry with exponential backoff
- [ ] Background processing via Edge Functions
- [ ] Efficient cursor-based pagination

### Setup & Configuration
- [x] **Set Up Supabase Project**: Create project and configure API settings
- [x] **Database Setup**: Enable extensions and run schema creation scripts
- [ ] **Environment Variables**: Configure Supabase and Readwise API credentials
- [ ] **Deploy Edge Functions**: sync-scheduler and incremental-sync
- [ ] **Set Up Scheduled Jobs**: Configure cron jobs for automatic syncing
- [ ] **API Rate Limiting**: Configure sync system for 20 requests/minute limit
- [ ] **Sync Frequency**: Configure appropriate sync intervals via cron jobs
- [ ] **Data Retention**: Configure cleanup policies for reading_events

### Dashboard Components
- [ ] **Overview Dashboard**: High-level metrics and trends
- [ ] **Reading Timeline**: Detailed session history
- [ ] **Content Library**: Browse and filter all documents
- [ ] **Highlights Hub**: Search and review all highlights
- [ ] **Analytics Deep Dive**: Advanced insights and patterns

### API Implementation
- [ ] **get_dashboard_data**: RPC function for dashboard metrics
- [ ] **get_reading_patterns**: RPC function for reading behavior analysis
- [ ] **category_stats**: View and queries for content categorization
- [ ] **Initial Sync**: Implement and test full data synchronization

### Monitoring & Performance
- [ ] **Health Checks**: sync_health and queue_performance views
- [ ] **Error Handling**: Automatic retry logic with exponential backoff
- [ ] **Database Optimization**: VACUUM, ANALYZE, and monitoring queries
- [ ] **Caching Strategy**: Implement multi-level caching with appropriate TTLs

### Documentation & Support
- [x] **Troubleshooting**: Documentation for common issues and solutions
- [x] **Contributing Guidelines**: Development setup and contribution process
- [x] **License**: MIT License documentation
- [ ] **Support Documentation**: Issue tracking, Discord, and Wiki links

## 🚀 Features

## 📋 Prerequisites

- [Supabase](https://supabase.com) account (free tier works)
- [Readwise](https://readwise.io) account with Reader API access
- Node.js 18+ (for local development)
- Basic knowledge of SQL and JavaScript

## 🛠️ Technology Stack

- **Backend**: Supabase (PostgreSQL + Edge Functions)
- **Time-Series Data**: TimescaleDB extension
- **Authentication**: Supabase Auth
- **Frontend**: React + TypeScript + Vite
- **UI Components**: Neobrutalism components from [https://www.neobrutalism.dev/](https://www.neobrutalism.dev/)
- **Charts**: D3.js, Chart.js, or Recharts
- **Styling**: TailwindCSS

## 📦 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/readwise-analytics.git
cd readwise-analytics
```

### 2. Set Up Supabase Project

1. Create a new project at [app.supabase.com](https://app.supabase.com)
2. Note your project URL and anon key from the API settings

### 3. Database Setup

1. Go to the SQL Editor in your Supabase dashboard
2. Run the following scripts in order:

```sql
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Run the schema creation script
-- (Copy the entire database schema from the architecture document)
```

### 4. Environment Variables

Create a `.env.local` file:

```env
# Supabase Configuration
NEXT_PUBLIC_SUPABASE_URL=your-project-url
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key

# Readwise API
READWISE_API_TOKEN=your-readwise-token
```

### 5. Deploy Edge Functions

```bash
# Install Supabase CLI
npm install -g supabase

# Link to your project
supabase link --project-ref your-project-ref

# Deploy sync functions
supabase functions deploy sync-scheduler
supabase functions deploy incremental-sync
```

### 6. Set Up Scheduled Jobs

In the Supabase dashboard, go to the SQL Editor and run:

```sql
-- Schedule sync tasks
SELECT cron.schedule(
    'process-sync-queue',
    '*/3 * * * *',
    $$
    SELECT net.http_post(
        url := 'https://your-project.supabase.co/functions/v1/sync-scheduler',
        headers := jsonb_build_object(
            'Authorization', 'Bearer ' || current_setting('app.service_role_key')
        )
    );
    $$
);
```

## 🔧 Configuration

### API Rate Limiting

The sync system is configured to respect Readwise's 20 requests/minute limit:

```javascript
// In edge functions/sync-scheduler.js
const RATE_LIMIT = 20 // requests per minute
const BATCH_SIZE = 15 // leave buffer for other operations
```

### Sync Frequency

Adjust sync intervals in the cron jobs:

```sql
-- Hourly incremental sync (default)
'0 * * * *'

-- Every 30 minutes
'*/30 * * * *'

-- Every 2 hours
'0 */2 * * *'
```

### Data Retention

Configure how long to keep detailed data:

```sql
-- Modify in cleanup job
DELETE FROM reading_events 
WHERE time < NOW() - INTERVAL '1 year'; -- Change retention period
```

## 📊 Usage

### Initial Sync

1. After deployment, trigger an initial full sync:

```javascript
// Run in your app or via Supabase dashboard
await supabase.functions.invoke('initial-sync', {
  body: { userId: 'your-user-id' }
})
```

2. Monitor sync progress:

```sql
SELECT * FROM sync_status WHERE user_id = 'your-user-id';
SELECT * FROM sync_queue WHERE user_id = 'your-user-id' ORDER BY created_at DESC;
```

### Viewing Analytics

The dashboard provides several key views:

1. **Overview Dashboard**: High-level metrics and trends
2. **Reading Timeline**: Detailed session history
3. **Content Library**: Browse and filter all documents
4. **Highlights Hub**: Search and review all highlights
5. **Analytics Deep Dive**: Advanced insights and patterns

### API Usage Examples

```javascript
// Get dashboard data
const { data } = await supabase.rpc('get_dashboard_data', {
  p_user_id: userId,
  p_days: 30
})

// Get reading patterns
const { data: patterns } = await supabase.rpc('get_reading_patterns', {
  p_user_id: userId,
  p_start_date: '2024-01-01',
  p_end_date: '2024-12-31'
})

// Get category statistics
const { data: categories } = await supabase
  .from('category_stats')
  .select('*')
  .eq('user_id', userId)
```

## 🔍 Monitoring

### Health Checks

Monitor system health via built-in views:

```sql
-- Check sync health
SELECT * FROM sync_health;

-- Monitor queue performance
SELECT * FROM queue_performance;

-- Check for failed syncs
SELECT * FROM sync_queue 
WHERE status = 'failed' 
AND created_at > NOW() - INTERVAL '24 hours';
```

### Error Handling

The system includes automatic retry logic:
- Failed syncs retry up to 3 times
- Exponential backoff prevents API flooding
- Error details stored for debugging

## 🚧 Troubleshooting

### Common Issues

1. **"Rate limit exceeded" errors**
    - Check if multiple users are syncing simultaneously
    - Reduce BATCH_SIZE in sync-scheduler function
    - Increase delay between requests

2. **Missing data after sync**
    - Verify Readwise API token permissions
    - Check sync_status table for errors
    - Review Edge Function logs in Supabase dashboard

3. **Slow dashboard loading**
    - Ensure materialized views are refreshed
    - Check if indexes are properly created
    - Consider implementing pagination for large datasets

### Debug Mode

Enable detailed logging:

```javascript
// In Edge Functions
const DEBUG = Deno.env.get('DEBUG') === 'true'
if (DEBUG) console.log('Detailed sync info...', data)
```

## 📈 Performance Optimization

### Database Optimization

1. **Vacuum regularly**:
   ```sql
   VACUUM ANALYZE documents;
   VACUUM ANALYZE reading_events;
   ```

2. **Update statistics**:
   ```sql
   ANALYZE;
   ```

3. **Monitor table sizes**:
   ```sql
   SELECT 
     schemaname,
     tablename,
     pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
   FROM pg_tables
   WHERE schemaname = 'public'
   ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
   ```

### Caching Strategy

- Dashboard data: 5-minute cache
- Document details: 1-minute cache
- User statistics: 10-minute cache
- Invalidate on new sync completion

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Readwise](https://readwise.io) for providing the Reader API
- [Supabase](https://supabase.com) for the backend infrastructure
- [TimescaleDB](https://timescale.com) for time-series capabilities

## 📮 Support

- Create an [Issue](https://github.com/yourusername/readwise-analytics/issues) for bug reports
- Join our [Discord](https://discord.gg/yourdiscord) for community support
- Check the [Wiki](https://github.com/yourusername/readwise-analytics/wiki) for detailed documentation

---

Built with ❤️ for the reading community