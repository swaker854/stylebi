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
package inetsoft.web.binding.dnd;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   property = "classType")
@JsonSubTypes({
   @JsonSubTypes.Type(value = TableTransfer.class,
                      name = "TableTransfer"),
   @JsonSubTypes.Type(value = CalcTableTransfer.class,
                      name = "CalcTableTransfer"),
   @JsonSubTypes.Type(value = ChartTransfer.class,
                      name = "ChartTransfer"),
   @JsonSubTypes.Type(value = ChartAestheticTransfer.class,
                      name = "ChartAestheticTransfer")
})
public class DataTransfer {
   public void setObjectType(String objectType) {
      this.objectType = objectType;
   }

   public String getObjectType() {
      return objectType;
   }

   public void setAssembly(String assembly) {
      this.assembly = assembly;
   }

   public String getAssembly() {
      return assembly;
   }

   private String objectType;
   private String assembly;
}