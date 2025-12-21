import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DrillDownParams,
  DrillDownResponse,
  WordsReadDocument,
  CompletedDocument,
  BacklogDocument,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DrillDownService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/analytics/drill-down';

  getWordsRead(params: DrillDownParams): Observable<DrillDownResponse<WordsReadDocument>> {
    return this.http.get<DrillDownResponse<WordsReadDocument>>(
      `${this.baseUrl}/words-read`,
      { params: this.buildParams(params) }
    );
  }

  getCompleted(params: DrillDownParams): Observable<DrillDownResponse<CompletedDocument>> {
    return this.http.get<DrillDownResponse<CompletedDocument>>(
      `${this.baseUrl}/completed`,
      { params: this.buildParams(params) }
    );
  }

  getBacklog(params: DrillDownParams): Observable<DrillDownResponse<BacklogDocument>> {
    return this.http.get<DrillDownResponse<BacklogDocument>>(
      `${this.baseUrl}/backlog`,
      { params: this.buildParams(params) }
    );
  }

  private buildParams(params: DrillDownParams): HttpParams {
    let httpParams = new HttpParams();

    if (params.startDate !== undefined) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate !== undefined) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.cursor !== undefined) {
      httpParams = httpParams.set('cursor', params.cursor);
    }
    if (params.limit !== undefined) {
      httpParams = httpParams.set('limit', params.limit.toString());
    }

    return httpParams;
  }
}
