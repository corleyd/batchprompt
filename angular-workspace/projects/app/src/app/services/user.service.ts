import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Observable, of, ReplaySubject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AccountService } from './account.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl: string;
  private userValidated = false;
  private currentUser: any = null;

  private creditBalanceSubject: ReplaySubject<number> = new ReplaySubject(1);
  public creditBalance$: Observable<number> = this.creditBalanceSubject.asObservable();

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private accountService: AccountService,
    private notificationService: NotificationService
  ) {
    this.apiUrl = `${environment.apiBaseUrl}/api/users`;
    
    // Listen for auth events to reset validation state on logout
    this.auth.isAuthenticated$.subscribe(isAuthenticated => {
      if (!isAuthenticated) {
        this.resetValidation();
      }
    });
  }

  /**
   * Validate the user upon login to ensure they have a user record
   * This will create a user record if none exists or update it if needed
   */
  validateUserOnLogin(): Observable<any> {
    // If the user is already validated, return the current user
    if (this.userValidated && this.currentUser) {
      return of(this.currentUser);
    }

    return this.http.post<any>(`${this.apiUrl}/login-validation`, null).pipe(
      tap(user => {
        this.userValidated = true;
        this.currentUser = user;
        console.log('User validated on login:', user);


        // Fetch accounts for the user and update credit balance
        if (user && user.userId) {
          this.accountService.getUserAccounts(user.userId).subscribe(accounts => {
            if (accounts && accounts.length > 0) {
              const firstAccount = accounts[0];
              this.accountService.getAccountBalance(firstAccount.accountUuid).subscribe(balance => {
                this.creditBalanceSubject.next(balance);
              });
            }
          });
        }

        this.notificationService.subscribeTo("account/balance")
          .subscribe((data: any) => {
          console.log('Balance update received:', data);
          let payload = data.payload;
          if (payload && payload.accountUuid) {
              this.creditBalanceSubject.next(payload.balance);
          }
        });

      }),
      catchError(error => {
        console.error('Error validating user on login:', error);
        this.userValidated = false;
        this.currentUser = null;
        throw error;
      })
    );
  }

  /**
   * Check if the user has been validated
   */
  isUserValidated(): boolean {
    return this.userValidated;
  }

  /**
   * Get the current user
   */
  getCurrentUser(): any {
    return this.currentUser;
  }

  /**
   * Reset the validation state (e.g., on logout)
   */
  resetValidation(): void {
    console.log('Resetting user validation state');
    this.userValidated = false;
    this.currentUser = null;
  }

  /**
   * Get a user by ID
   */
  getUserById(userId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${userId}`);
  }

  /**
   * Get a user by Auth0 ID
   */
  getUserByAuth0Id(userId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/auth0/${userId}`);
  }
  
  /**
   * Get all users with pagination and sorting
   */
  getAllUsers(page: number = 0, size: number = 10, sortBy: string = 'name', sortDirection: string = 'asc'): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDirection);
    
    return this.http.get<any>(this.apiUrl, { params });
  }
  
  /**
   * Search users by name with pagination and sorting
   */
  searchUsersByName(name: string, page: number = 0, size: number = 10, sortBy: string = 'name', sortDirection: string = 'asc'): Observable<any> {
    let params = new HttpParams()
      .set('name', name)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDirection);
    
    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }
  
  /**
   * Update a user
   */
  updateUser(userId: string, userData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${userId}`, userData);
  }
  
  /**
   * Delete a user
   */
  deleteUser(userId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${userId}`);
  }

}