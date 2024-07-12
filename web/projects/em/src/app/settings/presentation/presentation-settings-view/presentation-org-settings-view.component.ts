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
import { Component, ViewChild } from "@angular/core";
import { ContextHelp } from "../../../context-help";
import { Secured } from "../../../secured";
import { PresentationSettingsViewComponent } from "./presentation-settings-view.component";

@Secured({
   route: "/settings/presentation/org-settings",
   label: "Presentation Org Settings"
})
@ContextHelp({
   route: "/settings/presentation/org-settings",
   link: "EMSettingsPresentation"
})
@Component({
   selector: "em-presentation-org-settings-view",
   templateUrl: "./presentation-org-settings-view.component.html"
})
export class PresentationOrgSettingsViewComponent {
   @ViewChild("settingsView") settingsView: PresentationSettingsViewComponent;
}