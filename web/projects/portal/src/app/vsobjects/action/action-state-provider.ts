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
import { VSObjectModel } from "../model/vs-object-model";

/**
 * Interface for classes that provide contextual state information for actions.
 */
export interface ActionStateProvider {
   /**
    * Determines if an action is visible.
    *
    * @param id the action identifier.
    * @param model the object model.
    *
    * @return true if visible; false otherwise.
    */
   isActionVisible(id: string, model: VSObjectModel): boolean;

   /**
    * Determines if an action is enabled.
    *
    * @param id the action identifier.
    * @param model the object model.
    *
    * @return true if enabled; false otherwise.
    */
   isActionEnabled(id: string, model: VSObjectModel): boolean;
}