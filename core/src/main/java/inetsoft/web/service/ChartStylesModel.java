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
package inetsoft.web.service;

public class ChartStylesModel {
   public ChartStyle[] getStyles() {
      return styles;
   }

   public void setStyles(ChartStyle[] styles) {
      this.styles = styles;
   }

   public ChartStyle[] getStackStyles() {
      return stackStyles;
   }

   public void setStackStyles(ChartStyle[] stackStyles) {
      this.stackStyles = stackStyles;
   }

   private ChartStyle[] styles;
   private ChartStyle[] stackStyles;
}

class ChartStyle {
   public ChartStyle(String label, int data) {
      this.label = label;
      this.data = data;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   public int getData() {
      return data;
   }

   public void setData(int data) {
      this.data = data;
   }

   private String label;
   private int data;
}
