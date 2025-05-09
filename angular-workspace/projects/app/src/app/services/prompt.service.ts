import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Prompt } from '../models/prompt.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PromptService {
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/prompts`;
  }

  getAllPrompts(): Observable<Prompt[]> {
    return this.http.get<Prompt[]>(this.apiUrl);
  }

  getPromptById(promptUuid: string): Observable<Prompt> {
    return this.http.get<Prompt>(`${this.apiUrl}/${promptUuid}`);
  }

  getUserPrompts(
    userId?: string, 
    page: number = 0, 
    size: number = 10, 
    sort: string = 'createTimestamp', 
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

  searchPromptsByName(name: string): Observable<Prompt[]> {
    return this.http.get<Prompt[]>(`${this.apiUrl}/search?name=${name}`);
  }

  createPrompt(prompt: Prompt): Observable<Prompt> {
    return this.http.post<Prompt>(this.apiUrl, prompt);
  }

  updatePrompt(promptUuid: string, prompt: Prompt): Observable<Prompt> {
    return this.http.put<Prompt>(`${this.apiUrl}/${promptUuid}`, prompt);
  }

  deletePrompt(promptUuid: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${promptUuid}`);
  }
  
  // Admin function to copy a prompt to another user
  copyPromptToUser(sourcePromptUuid: string, targetUserId: string): Observable<any> {
    const url = `${this.apiUrl}/admin/copy`;
    const params = { sourcePromptUuid, targetUserId };
    return this.http.post(url, null, { params });
  }
}