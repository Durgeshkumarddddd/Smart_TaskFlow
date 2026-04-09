import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActivityLog } from '../models/task.model';

const API = `${environment.apiUrl}/activity`;

@Injectable({
  providedIn: 'root'
})
export class ActivityService {

  constructor(private http: HttpClient) {}

  getRecentActivity(limit: number = 20): Observable<ActivityLog[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ActivityLog[]>(API, { params });
  }
}
