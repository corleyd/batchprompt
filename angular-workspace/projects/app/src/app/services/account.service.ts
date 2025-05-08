import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/accounts`;
  }

  /**
   * Get account by UUID
   */
  getAccountById(accountUuid: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${accountUuid}`);
  }

  /**
   * Get account balance
   */
  getAccountBalance(accountUuid: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${accountUuid}/balance`);
  }

  /**
   * Get account transactions
   */
  getAccountTransactions(accountUuid: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${accountUuid}/transactions`);
  }

  /**
   * Add credits to an account
   */
  addCredits(accountUuid: string, amount: number, reason: string): Observable<any> {
    const transaction = {
      changeAmount: amount,
      reason: reason
    };
    return this.http.post<any>(`${this.apiUrl}/${accountUuid}/credits`, transaction);
  }

  /**
   * Create a new account
   */
  createAccount(accountName: string): Observable<any> {
    return this.http.post<any>(this.apiUrl, { name: accountName });
  }

  /**
   * Update an account
   */
  updateAccount(accountUuid: string, accountName: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${accountUuid}`, { name: accountName });
  }

  /**
   * Get accounts for a specific user (Admin only)
   */
  getUserAccounts(userId: string): Observable<any[]> {
    // This would require a custom endpoint on the backend
    return this.http.get<any[]>(`${this.apiUrl}/user/${userId}`);
  }
}