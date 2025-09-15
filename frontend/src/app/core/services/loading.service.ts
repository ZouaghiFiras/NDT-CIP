import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingSubject = new Subject<boolean>();
  public loading$: Observable<boolean> = this.loadingSubject.asObservable();

  /**
   * Set loading state
   * @param isLoading Loading state
   */
  setLoading(isLoading: boolean): void {
    this.loadingSubject.next(isLoading);
  }

  /**
   * Show loading indicator
   */
  show(): void {
    this.setLoading(true);
  }

  /**
   * Hide loading indicator
   */
  hide(): void {
    this.setLoading(false);
  }
}
