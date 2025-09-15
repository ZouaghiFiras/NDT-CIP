import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'ndt-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.scss']
})
export class AuthComponent implements OnInit {
  currentLang: string = 'en';

  constructor(
    private router: Router,
    private translate: TranslateService
  ) { }

  ngOnInit(): void {
    // Set language
    this.currentLang = this.translate.currentLang || 'en';
  }

  /**
   * Change language
   * @param lang Language code
   */
  changeLanguage(lang: string): void {
    this.currentLang = lang;
    this.translate.use(lang);
  }

  /**
   * Navigate to login
   */
  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  /**
   * Navigate to register
   */
  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }
}
