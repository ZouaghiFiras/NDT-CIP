import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private readonly TOKEN_KEY = 'auth-token';
  private readonly REFRESH_TOKEN_KEY = 'auth-refresh-token';
  private readonly USER_KEY = 'auth-user';

  constructor() { }

  /**
   * Save JWT token to storage
   * @param token JWT token
   */
  saveToken(token: string): void {
    if (token) {
      sessionStorage.setItem(this.TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(this.TOKEN_KEY);
    }
  }

  /**
   * Get JWT token from storage
   * @returns JWT token
   */
  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Save refresh token to storage
   * @param refreshToken Refresh token
   */
  saveRefreshToken(refreshToken: string): void {
    if (refreshToken) {
      sessionStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    } else {
      sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
  }

  /**
   * Get refresh token from storage
   * @returns Refresh token
   */
  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Save user data to storage
   * @param user User data
   */
  saveUser(user: any): void {
    if (user) {
      sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    } else {
      sessionStorage.removeItem(this.USER_KEY);
    }
  }

  /**
   * Get user data from storage
   * @returns User data
   */
  getUser(): any {
    const user = sessionStorage.getItem(this.USER_KEY);
    if (user) {
      return JSON.parse(user);
    }
    return null;
  }

  /**
   * Clear all authentication data from storage
   */
  signOut(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
  }

  /**
   * Check if user is authenticated
   * @returns True if authenticated, false otherwise
   */
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  /**
   * Get user roles
   * @returns Array of user roles
   */
  getUserRoles(): string[] {
    const user = this.getUser();
    return user ? user.roles : [];
  }

  /**
   * Check if user has specific role
   * @param role Role to check
   * @returns True if user has role, false otherwise
   */
  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  /**
   * Check if user has any of the specified roles
   * @param roles Roles to check
   * @returns True if user has any of the roles, false otherwise
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  /**
   * Check if user has all specified roles
   * @param roles Roles to check
   * @returns True if user has all roles, false otherwise
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role));
  }
}
