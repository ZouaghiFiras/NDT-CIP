import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToastrModule } from 'ngx-toastr';
import { PrimeNGConfig } from 'primeng/api';
import { HttpClient, HttpClientModule as HttpClientModule2 } from '@angular/common/http';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { AccordionModule } from 'primeng/accordion';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { MenuModule } from 'primeng/menu';
import { SidebarModule } from 'primeng/sidebar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { PaginatorModule } from 'primeng/paginator';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { MultiSelectModule } from 'primeng/multiselect';
import { ChipsModule } from 'primeng/chips';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputNumberModule } from 'primeng/inputnumber';
import { RadioButtonModule } from 'primeng/radiobutton';
import { CheckboxModule } from 'primeng/checkbox';
import { TreeModule } from 'primeng/tree';
import { ChartModule } from 'primeng/chart';
import { GalleriaModule } from 'primeng/galleria';
import { MessagesModule } from 'primeng/messages';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TriStateCheckboxModule } from 'primeng/tristatecheckbox';
import { DividerModule } from 'primeng/divider';
import { FieldsetModule } from 'primeng/fieldset';
import { BadgeModule } from 'primeng/badge';
import { AvatarModule } from 'primeng/avatar';
import { StepsModule } from 'primeng/steps';
import { TabMenuModule } from 'primeng/tabmenu';
import { TieredMenuModule } from 'primeng/tieredmenu';
import { OrderListModule } from 'primeng/orderlist';
import { VirtualScrollerModule } from 'primeng/virtualscroller';
import { CarouselModule } from 'primeng/carousel';
import { BlockUIModule } from 'primeng/blockui';
import { InplaceModule } from 'primeng/inplace';
import { ProgressBarModule } from 'primeng/progressbar';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { TerminalModule } from 'primeng/terminal';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { RatingModule } from 'primeng/rating';
import { SlideMenuModule } from 'primeng/slidemenu';
import { SpeedDialModule } from 'primeng/speeddial';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { ColorPickerModule } from 'primeng/colorpicker';
import { FileUploadModule } from 'primeng/fileupload';
import { EditorModule } from 'primeng/editor';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { ScrollerModule } from 'primeng/scroller';
import { DataViewModule } from 'primeng/dataview';
import { DataViewGridModule } from 'primeng/dataviewgrid';
import { TreeTableModule } from 'primeng/treetable';
import { ImageModule } from 'primeng/image';
import { AnimateModule } from 'primeng/animate';
import { NgxSpinnerModule } from 'ngx-spinner';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';
import { NgxPermissionsModule } from 'ngx-permissions';
import { TranslateModule } from '@ngx-translate/core';
import { NgxWebstorageModule } from 'ngx-webstorage';
import { NgxChartsModule } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LayoutModule } from './layout/layout.module';
import { AuthModule } from './auth/auth.module';
import { SharedModule } from './shared/shared.module';
import { CoreModule } from './core/core.module';
import { HTTP_INTERCEPTORS_PROVIDER } from './core/interceptors';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';
import { JwtInterceptor } from './core/interceptors/jwt.interceptor';
import { LoadingInterceptor } from './core/interceptors/loading.interceptor';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    BsDropdownModule.forRoot(),
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => {
          return new TranslateHttpLoader(http, './assets/i18n/', '.json');
        },
        deps: [HttpClient]
      }
    }),
    NgxWebstorageModule.forRoot(),
    NgxPermissionsModule.forRoot(),
    NgxChartsModule,
    NgxSpinnerModule,
    PerfectScrollbarModule,
    // PrimeNG Modules
    AccordionModule,
    TableModule,
    ToolbarModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    CardModule,
    DialogModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
    ToastModule,
    MenuModule,
    SidebarModule,
    BreadcrumbModule,
    PaginatorModule,
    CalendarModule,
    DropdownModule,
    MultiSelectModule,
    ChipsModule,
    InputSwitchModule,
    InputNumberModule,
    RadioButtonModule,
    CheckboxModule,
    TreeModule,
    ChartModule,
    GalleriaModule,
    MessagesModule,
    InputTextareaModule,
    SelectButtonModule,
    TriStateCheckboxModule,
    DividerModule,
    FieldsetModule,
    BadgeModule,
    AvatarModule,
    StepsModule,
    TabMenuModule,
    TieredMenuModule,
    OrderListModule,
    VirtualScrollerModule,
    CarouselModule,
    BlockUIModule,
    InplaceModule,
    ProgressBarModule,
    RippleModule,
    TooltipModule,
    TerminalModule,
    ToggleButtonModule,
    RatingModule,
    SlideMenuModule,
    SpeedDialModule,
    SkeletonModule,
    TagModule,
    ColorPickerModule,
    FileUploadModule,
    EditorModule,
    ScrollPanelModule,
    ScrollerModule,
    DataViewModule,
    DataViewGridModule,
    TreeTableModule,
    ImageModule,
    AnimateModule,
    ToastrModule.forRoot(),
    AppRoutingModule,
    LayoutModule,
    AuthModule,
    SharedModule,
    CoreModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HTTP_INTERCEPTORS_PROVIDER,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: LoadingInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
