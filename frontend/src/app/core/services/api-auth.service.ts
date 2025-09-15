import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { JwtResponse } from '../../auth/models/jwt-response';
import { LoginRequest } from '../../auth/models/login-request';
import { RegisterRequest } from '../../auth/models/register-request';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root'
})
export class ApiAuthService {
  private readonly apiUrl = environment.apiUrl;
  private currentUserSubject: BehaviorSubject<any>;
  public currentUser: Observable<any>;

  constructor(
    private http: HttpClient,
    private tokenStorageService: TokenStorageService
  ) {
    const storedUser = this.tokenStorageService.getUser();
    this.currentUserSubject = new BehaviorSubject<any>(storedUser);
    this.currentUser = this.currentUserSubject.asObservable();
  }

  /**
   * Get current user value
   */
  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }

  /**
   * Login user
   * @param credentials Login credentials
   * @returns Observable with JWT response
   */
  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/auth/signin`, credentials).pipe(
      map(response => {
        // Store tokens
        this.tokenStorageService.saveToken(response.token);
        this.tokenStorageService.saveRefreshToken(response.type === 'Bearer' ? response.token : '');

        // Store user
        const user = {
          username: response.username,
          email: response.email,
          roles: response.roles
        };
        this.tokenStorageService.saveUser(user);
        this.currentUserSubject.next(user);

        return response;
      })
    );
  }

  /**
   * Register new user
   * @param userData User registration data
   * @returns Observable with success message
   */
  register(userData: RegisterRequest): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/auth/signup`, userData);
  }

  /**
   * Refresh JWT token
   * @returns Observable with new JWT token
   */
  refreshToken(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/auth/refreshtoken`, {}, {
      headers: {
        'Authorization': `Bearer ${this.tokenStorageService.getRefreshToken()}`
      }
    }).pipe(
      tap(token => {
        this.tokenStorageService.saveToken(token);
      })
    );
  }

  /**
   * Logout user
   */
  logout(): void {
    // Clear tokens
    this.tokenStorageService.signOut();

    // Clear user
    this.currentUserSubject.next(null);
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    return !!this.tokenStorageService.getToken();
  }

  /**
   * Get user roles
   */
  getUserRoles(): string[] {
    const user = this.currentUserValue;
    return user ? user.roles : [];
  }

  /**
   * Check if user has specific role
   * @param role Role to check
   */
  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  /**
   * Check if user has any of the specified roles
   * @param roles Roles to check
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  /**
   * Check if user has all specified roles
   * @param roles Roles to check
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role));
  }
}
