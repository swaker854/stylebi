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
import { FormulaEditorDialog } from "./formula-editor-dialog.component";
import { ModalHeaderModule } from "../modal-header/modal-header.module";
import { MouseEventModule } from "../mouse-event/mouse-event.module";
import { ReactiveFormsModule } from "@angular/forms";
import { ScriptPaneModule } from "../dialog/script-pane/script-pane.module";
import { NewAggrDialogModule } from "../dialog/new-aggr-dialog/new-aggr-dialog.module";
import { WidgetDirectivesModule } from "../directive/widget-directives.module";
import { FormulaEditorService } from "./formula-editor.service";

@NgModule({
   imports: [
      CommonModule,
      ModalHeaderModule,
      MouseEventModule,
      ReactiveFormsModule,
      ScriptPaneModule,
      NewAggrDialogModule,
      WidgetDirectivesModule,
   ],
   declarations: [
      FormulaEditorDialog
   ],
   exports: [
      FormulaEditorDialog
   ],
   providers: [
      FormulaEditorService
   ],
})
export class FormulaEditorModule {
}