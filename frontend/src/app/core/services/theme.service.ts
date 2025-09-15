import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type Theme = 'light' | 'dark' | 'blue';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'app-theme';
  private currentTheme: BehaviorSubject<Theme>;
  private readonly defaultTheme: Theme = 'light';

  constructor() {
    const storedTheme = localStorage.getItem(this.THEME_KEY) as Theme;
    const initialTheme = storedTheme || this.defaultTheme;

    this.currentTheme = new BehaviorSubject<Theme>(initialTheme);
    this.applyTheme(initialTheme);
  }

  /**
   * Get current theme
   */
  getTheme(): Observable<Theme> {
    return this.currentTheme.asObservable();
  }

  /**
   * Get current theme value
   */
  getCurrentTheme(): Theme {
    return this.currentTheme.value;
  }

  /**
   * Set theme
   * @param theme Theme to set
   */
  setTheme(theme: Theme): void {
    this.applyTheme(theme);
    this.currentTheme.next(theme);
    localStorage.setItem(this.THEME_KEY, theme);
  }

  /**
   * Initialize theme
   */
  initTheme(): void {
    const storedTheme = localStorage.getItem(this.THEME_KEY);

    // Check system preference if no stored theme
    if (!storedTheme) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const theme: Theme = prefersDark ? 'dark' : 'light';
      this.setTheme(theme);
    } else {
      this.applyTheme(storedTheme as Theme);
    }

    // Listen for system theme changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      const currentTheme = this.getCurrentTheme();

      // Only follow system preference if using default theme
      if (currentTheme === 'light' || currentTheme === 'dark') {
        const newTheme: Theme = e.matches ? 'dark' : 'light';
        this.setTheme(newTheme);
      }
    });
  }

  /**
   * Apply theme to document
   * @param theme Theme to apply
   */
  private applyTheme(theme: Theme): void {
    const root = document.documentElement;

    // Remove all theme classes
    root.classList.remove('theme-light', 'theme-dark', 'theme-blue');

    // Add new theme class
    root.classList.add(`theme-${theme}`);

    // Set CSS variables based on theme
    this.setThemeVariables(theme);
  }

  /**
   * Set CSS variables for the theme
   * @param theme Theme to set variables for
   */
  private setThemeVariables(theme: Theme): void {
    const root = document.documentElement;

    // Define color schemes for each theme
    const themes: Record<Theme, Record<string, string>> = {
      light: {
        '--primary-color': '#1976d2',
        '--primary-hover': '#1565c0',
        '--secondary-color': '#424242',
        '--surface-color': '#ffffff',
        '--surface-hover': '#f5f5f5',
        '--text-primary': '#212121',
        '--text-secondary': '#757575',
        '--border-color': '#e0e0e0',
        '--success-color': '#4caf50',
        '--warning-color': '#ff9800',
        '--error-color': '#f44336',
        '--info-color': '#2196f3'
      },
      dark: {
        '--primary-color': '#90caf9',
        '--primary-hover': '#64b5f6',
        '--secondary-color': '#9e9e9e',
        '--surface-color': '#121212',
        '--surface-hover': '#1e1e1e',
        '--text-primary': '#ffffff',
        '--text-secondary': '#b0b0b0',
        '--border-color': '#424242',
        '--success-color': '#81c784',
        '--warning-color': '#ffb74d',
        '--error-color': '#e57373',
        '--info-color': '#64b5f6'
      },
      blue: {
        '--primary-color': '#0d47a1',
        '--primary-hover': '#0a3d91',
        '--secondary-color': '#546e7a',
        '--surface-color': '#f5f9ff',
        '--surface-hover': '#e8f1ff',
        '--text-primary': '#263238',
        '--text-secondary': '#546e7a',
        '--border-color': '#c5cae9',
        '--success-color': '#2e7d32',
        '--warning-color': '#f57c00',
        '--error-color': '#c62828',
        '--info-color': '#1565c0'
      }
    };

    // Apply theme variables
    Object.entries(themes[theme]).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
  }

  /**
   * Toggle between light and dark themes
   */
  toggleTheme(): void {
    const currentTheme = this.getCurrentTheme();
    let newTheme: Theme;

    if (currentTheme === 'light') {
      newTheme = 'dark';
    } else if (currentTheme === 'dark') {
      newTheme = 'blue';
    } else {
      newTheme = 'light';
    }

    this.setTheme(newTheme);
  }
}
