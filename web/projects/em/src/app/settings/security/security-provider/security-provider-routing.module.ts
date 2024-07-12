/*
 * inetsoft-web - StyleBI is a business intelligence web application.
 * Copyright © 2024 InetSoft Technology (info@inetsoft.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { CanComponentDeactivateService } from "../../../../../../shared/util/guard/can-component-deactivate.service";
import { AuthenticationProviderDetailPageComponent } from "./authentication-provider-detail-page/authentication-provider-detail-page.component";
import { AuthorizationProviderDetailPageComponent } from "./authorization-provider-detail-page/authorization-provider-detail-page.component";
import { SecurityProviderPageComponent } from "./security-provider-page/security-provider-page.component";

const routes: Routes = [
   {
      path: "show-authorization-provider",
      component: AuthorizationProviderDetailPageComponent,
      canDeactivate: [CanComponentDeactivateService]
   },
   {
      path: "show-authentication-provider",
      component: AuthenticationProviderDetailPageComponent,
      canDeactivate: [CanComponentDeactivateService]
   },
   {
      path: "",
      component: SecurityProviderPageComponent
   }
];

@NgModule({
   imports: [RouterModule.forChild(routes)],
   exports: [RouterModule],
   providers: [CanComponentDeactivateService]
})
export class SecurityProviderRoutingModule {
}
