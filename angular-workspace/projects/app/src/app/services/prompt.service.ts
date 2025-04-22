import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Prompt } from '../models/prompt.model';

@Injectable({
  providedIn: 'root'
})
export class PromptService {
  private apiUrl = 'http://localhost:8081/api/prompts';

  constructor(private http: HttpClient) { }

  getAllPrompts(): Observable<Prompt[]> {
    return this.http.get<Prompt[]>(this.apiUrl);
  }

  getPromptById(promptUuid: string): Observable<Prompt> {
    return this.http.get<Prompt>(`${this.apiUrl}/${promptUuid}`);
  }

  getUserPrompts(userId: string): Observable<Prompt[]> {
    return this.http.get<Prompt[]>(`${this.apiUrl}/user/${userId}`);
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
}