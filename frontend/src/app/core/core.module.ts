import { NgModule, Optional, SkipSelf } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { NgxPermissionsModule } from 'ngx-permissions';
import { NgxWebstorageModule } from 'ngx-webstorage';

import { throwIfAlreadyLoaded } from './module-import-guard';
import { HTTP_INTERCEPTORS_PROVIDER } from './interceptors';
import { ApiModule } from './services/api.module';
import { AuthModule } from './auth/auth.module';
import { ThemeModule } from './theme/theme.module';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    HttpClientModule,
    TranslateModule,
    NgxPermissionsModule,
    NgxWebstorageModule,
    ApiModule,
    AuthModule,
    ThemeModule,
    SharedModule
  ],
  providers: [
    HTTP_INTERCEPTORS_PROVIDER
  ]
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throwIfAlreadyLoaded(parentModule, 'CoreModule');
    }
  }
}
