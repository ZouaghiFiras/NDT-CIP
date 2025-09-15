import { Injectable, NgModule } from '@angular/core';

/**
 * This utility function ensures that a module is only loaded once.
 * It's particularly useful for singleton modules like CoreModule.
 * 
 * @param parentModule The parent module to check
 * @param moduleName The name of the module being imported
 */
export function throwIfAlreadyLoaded(parentModule: any, moduleName: string) {
  if (parentModule) {
    throw new Error(`${moduleName} has already been loaded. Import CoreModule in the AppModule only.`);
  }
}
