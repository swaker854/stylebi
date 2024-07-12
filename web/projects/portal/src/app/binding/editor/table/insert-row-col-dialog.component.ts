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
import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
   selector: "insert-row-col-dialog",
   templateUrl: "insert-row-col-dialog.component.html",
   styleUrls: ["insert-row-col-dialog.component.scss"]
})

export class InsertRowColDialog {
   @Input() helpLinkKey: string = "FreehandTableVS";
   public insertRadio: string = "true";
   public insertBefore: string = "true";
   public num: number = 1;
   @Output() onApply = new EventEmitter<{collapse: boolean, result: any}>();

   changeNum(evt: any) {
      if(evt.target.value <= 0) {
         this.num = 1;
      }
   }

   @Output() onCommit: EventEmitter<any> = new EventEmitter<any>();
   @Output() onCancel: EventEmitter<string> = new EventEmitter<string>();

   cancelChanges(): void {
      this.onCancel.emit("cancel");
   }

   ok(): void {
      this.onCommit.emit({
         insertRadio: this.insertRadio,
         insertBefore: this.insertBefore,
         num: this.num
      });
   }

   apply(event: boolean): void {
      this.onApply.emit({collapse: event, result: {
         insertRadio: this.insertRadio,
         insertBefore: this.insertBefore,
         num: this.num
      }});
   }
}