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
import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatDialogModule } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatGridListModule } from "@angular/material/grid-list";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatListModule } from "@angular/material/list";
import { MatSelectModule } from "@angular/material/select";
import { MatSidenavModule } from "@angular/material/sidenav";
import { EditorPanelModule } from "../../../common/util/editor-panel/editor-panel.module";
import { ErrorHandlerService } from "../../../common/util/error/error-handler.service";
import { MessageDialogModule } from "../../../common/util/message-dialog.module";
import { TopScrollModule } from "../../../top-scroll/top-scroll.module";
import { EmailPickerModule } from "../../email-picker/email-picker.module";
import { SecurityTableViewModule } from "../security-table-view/security-table-view.module";
import { SecurityTreeViewModule } from "../security-tree-view/security-tree-view.module";
import { EditIdentityPaneComponent } from "./edit-identity-pane/edit-identity-pane.component";
import { EditIdentityViewComponent } from "./edit-identity-view/edit-identity-view.component";
import { IdentityTablesPaneComponent } from "./identity-tables-pane/identity-tables-pane.component";
import { SecurityTreeService } from "./security-tree.service";
import { UsersSettingsPageComponent } from "./users-settings-page/users-settings-page.component";
import { UsersSettingsSaveGuard } from "./users-settings-page/users-settings-save.guard";
import { UsersSettingsRoutingModule } from "./users-settings-routing.module";
import { UsersSettingsViewComponent } from "./users-settings-view/users-settings-view.component";
import { PropertyTableViewModule } from "../property-table-view/property-table-view.module";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";

@NgModule({
   imports: [
      CommonModule,
      FormsModule,
      MatCardModule,
      MatCheckboxModule,
      MatDividerModule,
      MatFormFieldModule,
      MatGridListModule,
      MatIconModule,
      MatInputModule,
      MatListModule,
      MatSelectModule,
      ReactiveFormsModule,
      SecurityTreeViewModule,
      UsersSettingsRoutingModule,
      SecurityTableViewModule,
      PropertyTableViewModule,
      MatButtonModule,
      EmailPickerModule,
      EditorPanelModule,
      MatDialogModule,
      MessageDialogModule,
      MatSidenavModule,
      TopScrollModule,
      MatSlideToggleModule
   ],
   declarations: [
      EditIdentityPaneComponent,
      IdentityTablesPaneComponent,
      UsersSettingsPageComponent,
      UsersSettingsViewComponent,
      EditIdentityViewComponent
   ],
   providers: [
      SecurityTreeService,
      ErrorHandlerService,
      UsersSettingsSaveGuard
   ]
})
export class UsersSettingsModule {
}
