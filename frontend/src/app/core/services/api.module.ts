import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { ApiAuthService } from './api-auth.service';
import { ApiService } from './api.service';
import { ApiDeviceService } from '../features/devices/services/api-device.service';
import { ApiTopologyService } from '../features/topology/services/api-topology.service';
import { ApiSimulationService } from '../features/simulation/services/api-simulation.service';
import { ApiRiskService } from '../features/risk/services/api-risk.service';
import { ApiService as PolicyService } from '../features/policy/services/api-policy.service';
import { ApiService as ParametricService } from '../features/parametric/services/api-parametric.service';
import { ApiService as ThreatsService } from '../features/threats/services/api-threats.service';
import { ApiService as LogsService } from '../features/logs/services/api-logs.service';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    HttpClientModule
  ],
  providers: [
    ApiAuthService,
    ApiService,
    ApiDeviceService,
    ApiTopologyService,
    ApiSimulationService,
    ApiRiskService,
    PolicyService,
    ParametricService,
    ThreatsService,
    LogsService
  ]
})
export class ApiModule { }
