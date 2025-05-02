import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/jobs`;
  }

  // Get all supported models
  getSupportedModels(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/models`);
  }

  // Submit a job
  submitJob(fileUuid: string, promptUuid: string, modelName: string, outputFieldUuids?: string[]): Observable<any> {
    const payload = {
      fileUuid,
      promptUuid,
      modelName,
      outputFieldUuids
    };
    return this.http.post(`${this.apiUrl}/submit`, payload);
  }

  // Get jobs for current user with pagination and sorting
  getUserJobs(
    page: number = 0, 
    size: number = 20, 
    sort: string = 'updatedAt', 
    direction: string = 'desc'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('direction', direction);
    
    return this.http.get<any>(`${this.apiUrl}/user`, { params });
  }

  // Get job by ID
  getJobById(jobUuid: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${jobUuid}`);
  }

  // Get tasks for a job
  getJobTasks(jobUuid: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${jobUuid}/tasks`);
  }
}