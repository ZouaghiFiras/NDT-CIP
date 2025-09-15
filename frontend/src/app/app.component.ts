import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from './auth/services/auth.service';
import { StorageService } from './core/services/storage.service';
import { ThemeService } from './core/services/theme.service';
import { NotificationService } from './core/services/notification.service';

@Component({
  selector: 'ndt-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'Network Digital Twin Cyber Insurance Platform';

  constructor(
    private router: Router,
    private primeNGConfig: PrimeNGConfig,
    private translate: TranslateService,
    private authService: AuthService,
    private storageService: StorageService,
    private themeService: ThemeService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    // Initialize language
    this.translate.addLangs(['en', 'fr', 'es']);
    this.translate.setDefaultLang('en');

    // Set theme
    this.themeService.initTheme();

    // Initialize PrimeNG
    this.primeNGConfig.setTranslation(this.translate.instant('primeng'));

    // Initialize notifications
    this.notificationService.init();

    // Check if user is logged in
    const currentUser = this.storageService.getUser();
    if (currentUser) {
      this.authService.autoLogin();
    }
  }
}
