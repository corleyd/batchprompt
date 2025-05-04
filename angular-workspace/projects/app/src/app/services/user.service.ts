import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthService } from '@auth0/auth0-angular';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl: string;
  private userValidated = false;
  private currentUser: any = null;

  constructor(
    private http: HttpClient,
    private auth: AuthService
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
  getUserById(userUuid: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${userUuid}`);
  }

  /**
   * Get a user by Auth0 ID
   */
  getUserByAuth0Id(userId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/auth0/${userId}`);
  }
}