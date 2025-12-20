import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SyncResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class SyncService {
  private readonly http = inject(HttpClient);

  triggerSync(): Observable<SyncResponse> {
    return this.http.post<SyncResponse>('/sync', {});
  }
}
