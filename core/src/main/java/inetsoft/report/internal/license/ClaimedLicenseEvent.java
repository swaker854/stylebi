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
package inetsoft.report.internal.license;

import java.util.EventObject;

/**
 * Event that signals that the claimed license for this instance has changed.
 */
public class ClaimedLicenseEvent extends EventObject {
   /**
    * Creates a new instance of {@code ClaimedLicenseEvent}.
    *
    * @param source  the source of the event.
    * @param license the newly claimed license.
    */
   public ClaimedLicenseEvent(Object source, License license) {
      super(source);
      this.license = license;
   }

   /**
    * Gets the newly claimed license.
    *
    * @return the license.
    */
   public License getLicense() {
      return license;
   }

   private final License license;
}