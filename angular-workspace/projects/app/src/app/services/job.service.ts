import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private apiUrl = 'http://localhost:8081/api/jobs'; // Adjust base URL as needed

  constructor(private http: HttpClient) { }

  // Get all supported models
  getSupportedModels(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/models`);
  }

  // Submit a job
  submitJob(fileUuid: string, promptUuid: string, modelName: string): Observable<any> {
    const payload = {
      fileUuid,
      promptUuid,
      modelName
    };
    return this.http.post(`${this.apiUrl}/submit`, payload);
  }

  // Get jobs for current user
  getUserJobs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user`);
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