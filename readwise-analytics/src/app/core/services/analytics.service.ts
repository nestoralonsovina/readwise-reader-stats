import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DashboardResponse,
  ReadingStatsResponse,
  StreakResponse,
  PeakHoursResponse,
  PipelineResponse,
  HighlightResponse,
  Granularity,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/analytics';

  getDashboard(days: number): Observable<DashboardResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<DashboardResponse>(`${this.baseUrl}/dashboard`, { params });
  }

  getReadingStats(
    granularity: Granularity,
    startDate?: string,
    endDate?: string
  ): Observable<ReadingStatsResponse> {
    let params = new HttpParams().set('granularity', granularity);
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<ReadingStatsResponse>(`${this.baseUrl}/reading/stats`, { params });
  }

  getStreak(): Observable<StreakResponse> {
    return this.http.get<StreakResponse>(`${this.baseUrl}/reading/streak`);
  }

  getPeakHours(days: number): Observable<PeakHoursResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<PeakHoursResponse>(`${this.baseUrl}/reading/peak-hours`, { params });
  }

  getPipeline(days: number): Observable<PipelineResponse> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<PipelineResponse>(`${this.baseUrl}/pipeline`, { params });
  }

  getHighlights(days: number, topDocumentsLimit = 10): Observable<HighlightResponse> {
    const params = new HttpParams()
      .set('days', days.toString())
      .set('topDocumentsLimit', topDocumentsLimit.toString());
    return this.http.get<HighlightResponse>(`${this.baseUrl}/highlights`, { params });
  }
}
