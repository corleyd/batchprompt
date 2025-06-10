import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WaitlistSignupDto {
  email: string;
  name?: string;
  company?: string;
  useCase?: string;
}

export interface WaitlistEntryDto {
  id: string;
  email: string;
  name?: string;
  company?: string;
  useCase?: string;
  position?: number;
  status: 'PENDING' | 'INVITED' | 'REGISTERED';
  createdAt: string;
  invitedAt?: string;
  registeredAt?: string;
}

export interface WaitlistAutoAcceptanceDto {
  id?: string;
  remainingAutoAcceptCount: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  notes?: string;
}

export interface SetAutoAcceptanceCountDto {
  count: number;
  notes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WaitlistService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/waitlist`;

  constructor(private http: HttpClient) { }

  joinWaitlist(signupData: WaitlistSignupDto): Observable<WaitlistEntryDto> {
    return this.http.post<WaitlistEntryDto>(`${this.baseUrl}/public/join`, signupData);
  }

  getWaitlistStatus(email: string): Observable<WaitlistEntryDto> {
    return this.http.get<WaitlistEntryDto>(`${this.baseUrl}/public/status`, {
      params: { email }
    });
  }

  getWaitlistPosition(email: string): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/public/position`, {
      params: { email }
    });
  }

  markAsRegistered(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/public/register`, null, {
      params: { email }
    });
  }

  // Admin methods
  getAllEntries(): Observable<WaitlistEntryDto[]> {
    return this.http.get<WaitlistEntryDto[]>(`${environment.apiBaseUrl}/api/waitlist/admin/entries`);
  }

  getPendingEntries(): Observable<WaitlistEntryDto[]> {
    return this.http.get<WaitlistEntryDto[]>(`${environment.apiBaseUrl}/api/waitlist/admin/pending`);
  }

  inviteUser(entryId: string): Observable<WaitlistEntryDto> {
    return this.http.post<WaitlistEntryDto>(`${environment.apiBaseUrl}/api/waitlist/admin/invite/${entryId}`, null);
  }

  inviteNextUsers(count: number): Observable<WaitlistEntryDto[]> {
    return this.http.post<WaitlistEntryDto[]>(`${environment.apiBaseUrl}/api/waitlist/admin/invite-next`, null, {
      params: { count: count.toString() }
    });
  }

  getAutoAcceptanceConfiguration(): Observable<WaitlistAutoAcceptanceDto> {
    return this.http.get<WaitlistAutoAcceptanceDto>(`${environment.apiBaseUrl}/api/waitlist/admin/auto-acceptance`);
  }

  setAutoAcceptanceCount(request: SetAutoAcceptanceCountDto): Observable<WaitlistAutoAcceptanceDto> {
    return this.http.post<WaitlistAutoAcceptanceDto>(`${environment.apiBaseUrl}/api/waitlist/admin/auto-acceptance`, request);
  }
}