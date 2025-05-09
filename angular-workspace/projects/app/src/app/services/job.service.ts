import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface JobSubmissionDto {
  fileUuid: string;
  promptUuid: string;
  modelId: string;
  outputFieldUuids?: string[];
  maxTokens?: number;
  temperature?: number;
  maxRecords?: number;
  startRecordNumber?: number;
  targetUserId?: string; // User ID when submitting on behalf of another user
}

export interface ModelDto {
  modelId: string;
  name: string;
  provider: string;
  providerDisplayName: string;
  queue: string;
  properties: {[key: string]: any};
}

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/jobs`;
  }

  // Get all supported models
  getSupportedModels(): Observable<ModelDto[]> {
    return this.http.get<ModelDto[]>(`${this.apiUrl}/models`);
  }

  // Submit a job with all parameters
  submitJob(submission: JobSubmissionDto): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit`, submission);
  }

  // Get jobs for current user with pagination and sorting
  getUserJobs(
    userId?: string,
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
    let url = `${this.apiUrl}/user`;
    if (userId) {
      url += `/${userId}`;
    }
    
    return this.http.get<any>(url, { params });
  }

  // Get job by ID
  getJobById(jobUuid: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${jobUuid}`);
  }

  // Get tasks for a job with pagination and sorting
  getJobTasks(
    jobUuid: string,
    page: number = 0, 
    size: number = 20, 
    sort: string = 'createdAt', 
    direction: string = 'desc'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('direction', direction);
    
    return this.http.get<any>(`${this.apiUrl}/${jobUuid}/tasks`, { params });
  }
}