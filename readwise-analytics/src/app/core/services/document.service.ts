import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentDetailResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/documents';

  getDocument(id: string): Observable<DocumentDetailResponse> {
    return this.http.get<DocumentDetailResponse>(`${this.baseUrl}/${id}`);
  }
}
