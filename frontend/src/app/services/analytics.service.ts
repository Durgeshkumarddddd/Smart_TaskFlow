import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TaskSummary } from '../models/task.model';

const API = `${environment.apiUrl}/tasks/summary`;

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {

  constructor(private http: HttpClient) {}

  getSummary(): Observable<TaskSummary> {
    return this.http.get<TaskSummary>(API);
  }
}
