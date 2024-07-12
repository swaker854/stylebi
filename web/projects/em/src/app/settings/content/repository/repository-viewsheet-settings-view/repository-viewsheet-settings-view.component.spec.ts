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
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatOptionModule } from "@angular/material/core";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatGridListModule } from "@angular/material/grid-list";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatRadioModule } from "@angular/material/radio";
import { MatSelectModule } from "@angular/material/select";
import { MatTableModule } from "@angular/material/table";
import { MatTabsModule } from "@angular/material/tabs";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterTestingModule } from "@angular/router/testing";
import { LoadingSpinnerModule } from "../../../../common/util/loading-spinner/loading-spinner.module";
import { TableView } from "../../../../common/util/table/table-view.component";
import { ResourcePermissionModule } from "../../../security/resource-permission/resource-permission.module";
import { AnalyzeMvPageComponent } from "../analyze-mv-page/analyze-mv-page.component";
import { RepositorySheetSettingsViewComponent } from "../repository-sheet-settings-view/repository-sheet-settings-view.component";
import { RepositoryViewsheetSettingsViewComponent } from "./repository-viewsheet-settings-view.component";

describe("RepositoryViewsheetSettingsViewComponent", () => {
   let component: RepositoryViewsheetSettingsViewComponent;
   let fixture: ComponentFixture<RepositoryViewsheetSettingsViewComponent>;

   beforeEach(() => {
      TestBed.configureTestingModule({
         imports: [
            HttpClientTestingModule,
            RouterTestingModule,
            NoopAnimationsModule,
            FormsModule,
            ReactiveFormsModule,
            MatButtonModule,
            MatCardModule,
            MatCheckboxModule,
            MatExpansionModule,
            MatFormFieldModule,
            MatGridListModule,
            MatInputModule,
            MatOptionModule,
            MatProgressSpinnerModule,
            MatRadioModule,
            MatSelectModule,
            MatTableModule,
            MatTabsModule,
            LoadingSpinnerModule,
            ResourcePermissionModule
         ],
         declarations: [
            AnalyzeMvPageComponent,
            RepositorySheetSettingsViewComponent,
            RepositoryViewsheetSettingsViewComponent,
            TableView
         ],
         schemas: [
            NO_ERRORS_SCHEMA
         ]
      })
      .compileComponents();

      fixture = TestBed.createComponent(RepositoryViewsheetSettingsViewComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
   });

   it("should create", () => {
      expect(component).toBeTruthy();
   });
});
