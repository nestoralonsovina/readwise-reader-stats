// Period info shared across responses
export interface PeriodInfo {
  readonly startDate: string;
  readonly endDate: string;
  readonly label: string;
}

// Dashboard endpoint
export interface SummaryDto {
  readonly wordsRead: number;
  readonly articlesCompleted: number;
  readonly currentStreak: number;
  readonly backlogSize: number;
  readonly highlightsCreated: number;
}

export interface QuickStatsDto {
  readonly completionRate: number;
}

export interface DashboardResponse {
  readonly period: PeriodInfo;
  readonly summary: SummaryDto;
  readonly quickStats: QuickStatsDto;
}

// Reading stats endpoint
export interface DailyStatsDto {
  readonly date: string;
  readonly wordsRead: number;
  readonly articlesProgressed: number;
  readonly articlesCompleted: number;
}

export interface TotalsDto {
  readonly totalWordsRead: number;
  readonly totalArticlesCompleted: number;
}

export interface ReadingStatsResponse {
  readonly period: PeriodInfo;
  readonly granularity: string;
  readonly stats: readonly DailyStatsDto[];
  readonly totals: TotalsDto;
}

// Streak endpoint
export interface StreakDto {
  readonly days: number;
  readonly startDate: string | null;
  readonly endDate: string | null;
}

export interface StreakResponse {
  readonly current: StreakDto;
  readonly longest: StreakDto;
}

// Peak hours endpoint
export interface HourlyActivityDto {
  readonly hour: number;
  readonly label: string;
  readonly activityCount: number;
  readonly percentage: number;
}

export interface PeakHoursResponse {
  readonly distribution: readonly HourlyActivityDto[];
  readonly peakHour: number;
  readonly peakHourLabel: string;
  readonly peakPercentage: number;
}

// Pipeline endpoint
export interface CurrentPipelineDto {
  readonly backlog: number;
  readonly inProgress: number;
  readonly completed: number;
  readonly archived: number;
  readonly total: number;
}

export interface PipelinePeriodDto {
  readonly documentsAdded: number;
  readonly documentsCompleted: number;
  readonly saveToReadRatio: number;
  readonly averageQueueLatencyHours: number | null;
}

export interface LocationDto {
  readonly location: string;
  readonly count: number;
  readonly percentage: number;
}

export interface CategoryDto {
  readonly category: string;
  readonly count: number;
  readonly averageProgress: number;
  readonly averageProgressPercent: string;
}

export interface BreakdownDto {
  readonly byLocation: readonly LocationDto[];
  readonly byCategory: readonly CategoryDto[];
}

export interface PipelineResponse {
  readonly current: CurrentPipelineDto;
  readonly period: PipelinePeriodDto;
  readonly breakdown: BreakdownDto;
}

// Highlights endpoint
export interface HighlightSummaryDto {
  readonly total: number;
  readonly withNotes: number;
  readonly notePercentage: number;
  readonly thisPeriod: number;
  readonly previousPeriod: number;
  readonly periodChange: number;
  readonly periodChangePercent: number | null;
  readonly averagePerDocument: number;
}

export interface TopDocumentDto {
  readonly documentId: string;
  readonly title: string | null;
  readonly category: string | null;
  readonly highlightCount: number;
  readonly imageUrl: string | null;
  readonly hasNotes: boolean;
}

export interface HighlightResponse {
  readonly summary: HighlightSummaryDto;
  readonly topDocuments: readonly TopDocumentDto[];
}

// Sync endpoint
export type SyncStatus = 'STARTED' | 'COMPLETED' | 'FAILED';

export interface SyncResponse {
  readonly syncId: string;
  readonly status: SyncStatus;
  readonly startedAt: string;
  readonly completedAt: string | null;
  readonly documentsProcessed: number;
  readonly highlightsProcessed: number;
  readonly errorMessage: string | null;
}

// UI state types
export type FixedPeriod = 7 | 30 | 365;

export interface CustomDateRange {
  readonly type: 'custom';
  readonly startDate: string;
  readonly endDate: string;
}

export type Period = FixedPeriod | CustomDateRange;
export type Granularity = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export function isCustomPeriod(period: Period): period is CustomDateRange {
  return typeof period === 'object' && period.type === 'custom';
}

export function periodToGranularity(period: Period): Granularity {
  if (isCustomPeriod(period)) {
    return 'DAILY';
  }
  switch (period) {
    case 7:
      return 'DAILY';
    case 30:
      return 'DAILY';
    case 365:
      return 'WEEKLY';
    default: {
      const exhaustiveCheck: never = period;
      throw new Error(`Unsupported period: ${exhaustiveCheck}`);
    }
  }
}

export function periodToLabel(period: Period): string {
  if (isCustomPeriod(period)) {
    return 'Custom';
  }
  switch (period) {
    case 7:
      return 'Week';
    case 30:
      return 'Month';
    case 365:
      return 'Year';
    default: {
      const exhaustiveCheck: never = period;
      throw new Error(`Unsupported period: ${exhaustiveCheck}`);
    }
  }
}

export function periodToDateRange(period: Period): { startDate: string; endDate: string } {
  if (isCustomPeriod(period)) {
    return { startDate: period.startDate, endDate: period.endDate };
  }
  const endDate = new Date();
  const startDate = new Date();
  startDate.setDate(endDate.getDate() - period);

  return {
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
  };
}

// ============================================================================
// Drill-Down Types
// ============================================================================

export type DrillDownType = 'words' | 'completed' | 'backlog';

export interface DrillDownParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly cursor?: string;
  readonly limit?: number;
}

export interface DrillDownSummary {
  readonly total: number;
  readonly changePercent: number | null;
}

// Base document interface for drill-down lists
export interface DrillDownDocument {
  readonly id: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string | null;
}

export interface WordsReadDocument extends DrillDownDocument {
  readonly wordsRead: number;
  readonly readingProgress: number;
}

export interface CompletedDocument extends DrillDownDocument {
  readonly completedAt: string;
}

export interface BacklogDocument extends DrillDownDocument {
  readonly savedAt: string;
  readonly daysWaiting: number;
}

export interface DrillDownResponse<T extends DrillDownDocument> {
  readonly summary: DrillDownSummary;
  readonly documents: readonly T[];
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
}

// ============================================================================
// Document Detail Types
// ============================================================================

export interface HighlightDto {
  readonly id: string;
  readonly text: string;
  readonly note: string | null;
  readonly createdAt: string | null;
}

export interface DocumentStatsDto {
  readonly highlightCount: number;
  readonly notesCount: number;
  readonly estimatedReadingTime: number;
}

export interface DocumentDetailResponse {
  readonly id: string;
  readonly readwiseId: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly sourceUrl: string;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string | null;
  readonly location: string | null;
  readonly wordCount: number | null;
  readonly readingProgress: number;
  readonly savedAt: string | null;
  readonly firstOpenedAt: string | null;
  readonly lastOpenedAt: string | null;
  readonly tags: readonly string[];
  readonly highlights: readonly HighlightDto[];
  readonly stats: DocumentStatsDto;
}
