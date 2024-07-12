/*
 * inetsoft-core - StyleBI is a business intelligence web application.
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
package inetsoft.web.viewsheet.event;

import javax.annotation.Nullable;

public class CheckBaseWsChangedEvent implements ViewsheetEvent {
   /**
    * @return the assembly name
    */
   @Nullable
   public String name() {
      return null;
   }

   /**
    * Check if the event was confirmed by a message dialog
    *
    * @return if the event was confirmed
    */
   public boolean confirmed() {
      return false;
   }
}