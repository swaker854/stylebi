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
package inetsoft.web.viewsheet.command;

import inetsoft.uql.asset.Assembly;

import java.util.List;

/**
 * Class that refresh embedded viewsheet.
 *
 * @since 12.3
 */
public class RefreshEmbeddedVSCommand implements ViewsheetCommand {
   public RefreshEmbeddedVSCommand(List assemblies) {
      this.assemblies = (String[]) assemblies.stream()
         .map(a -> ((Assembly) a).getAbsoluteName())
         .toArray(String[]::new);
   }

   public String[] getAssemblies() {
      return assemblies;
   }

   private String[] assemblies;
}
