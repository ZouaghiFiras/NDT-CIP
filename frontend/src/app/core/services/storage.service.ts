import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  /**
   * Get item from localStorage
   * @param key Storage key
   * @returns Parsed value or null if not found
   */
  get(key: string): any {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : null;
    } catch (error) {
      console.error(`Error getting item from localStorage with key ${key}:`, error);
      return null;
    }
  }

  /**
   * Set item to localStorage
   * @param key Storage key
   * @param value Value to store
   */
  set(key: string, value: any): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error(`Error setting item to localStorage with key ${key}:`, error);
    }
  }

  /**
   * Remove item from localStorage
   * @param key Storage key
   */
  remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch (error) {
      console.error(`Error removing item from localStorage with key ${key}:`, error);
    }
  }

  /**
   * Clear all items from localStorage
   */
  clear(): void {
    try {
      localStorage.clear();
    } catch (error) {
      console.error('Error clearing localStorage:', error);
    }
  }

  /**
   * Check if localStorage has a specific key
   * @param key Storage key
   * @returns True if key exists, false otherwise
   */
  has(key: string): boolean {
    return localStorage.getItem(key) !== null;
  }

  /**
   * Get all keys in localStorage
   * @returns Array of keys
   */
  keys(): string[] {
    const keys: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      keys.push(localStorage.key(i)!);
    }
    return keys;
  }

  /**
   * Get the size of localStorage in bytes
   * @returns Size in bytes
   */
  size(): number {
    let size = 0;
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)!;
      const value = localStorage.getItem(key);
      size += key.length + value!.length;
    }
    return size;
  }

  /**
   * Check if localStorage is available
   * @returns True if available, false otherwise
   */
  isAvailable(): boolean {
    try {
      const testKey = '__test__';
      localStorage.setItem(testKey, testKey);
      localStorage.removeItem(testKey);
      return true;
    } catch (e) {
      return false;
    }
  }

  /**
   * Get item from sessionStorage
   * @param key Storage key
   * @returns Parsed value or null if not found
   */
  getSession(key: string): any {
    try {
      const item = sessionStorage.getItem(key);
      return item ? JSON.parse(item) : null;
    } catch (error) {
      console.error(`Error getting item from sessionStorage with key ${key}:`, error);
      return null;
    }
  }

  /**
   * Set item to sessionStorage
   * @param key Storage key
   * @param value Value to store
   */
  setSession(key: string, value: any): void {
    try {
      sessionStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error(`Error setting item to sessionStorage with key ${key}:`, error);
    }
  }

  /**
   * Remove item from sessionStorage
   * @param key Storage key
   */
  removeSession(key: string): void {
    try {
      sessionStorage.removeItem(key);
    } catch (error) {
      console.error(`Error removing item from sessionStorage with key ${key}:`, error);
    }
  }

  /**
   * Clear all items from sessionStorage
   */
  clearSession(): void {
    try {
      sessionStorage.clear();
    } catch (error) {
      console.error('Error clearing sessionStorage:', error);
    }
  }

  /**
   * Check if sessionStorage has a specific key
   * @param key Storage key
   * @returns True if key exists, false otherwise
   */
  hasSession(key: string): boolean {
    return sessionStorage.getItem(key) !== null;
  }

  /**
   * Get all keys in sessionStorage
   * @returns Array of keys
   */
  sessionKeys(): string[] {
    const keys: string[] = [];
    for (let i = 0; i < sessionStorage.length; i++) {
      keys.push(sessionStorage.key(i)!);
    }
    return keys;
  }
}
