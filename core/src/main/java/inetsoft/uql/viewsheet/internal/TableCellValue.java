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
package inetsoft.uql.viewsheet.internal;

import inetsoft.util.Tool;
import org.w3c.dom.Element;

import java.io.PrintWriter;

class TableCellValue extends AnnotationCellValue {
   /**
    * The type of annotation cell.
    */
   @Override
   public int getType() {
      return NORMAL_TABLE;
   }

   /**
    * Get column header name.
    */
   public String getColHeader() {
      return colHeader;
   }

   /**
    * Set column header name.
    */
   public void setColHeader(String colHeader) {
      this.colHeader = colHeader;
   }

   /**
    * Set repeat row index.
    */
   public void setRepeatedRowIndex(int idx) {
      this.repRowIdx = idx;
   }

   /**
    * Get repeat row index.
    */
   public int getRepeatedRowIndex() {
      return repRowIdx;
   }

   /**
    * Set repeat col index.
    */
   public void setRepeatedColIndex(int idx) {
      this.repColIdx = idx;
   }

   /**
    * Get repeat col index.
    */
   public int getRepeatedColIndex() {
      return repColIdx;
   }

   /**
    * Write contents.
    * @param writer the specified writer.
    */
   @Override
   protected void writeContents(PrintWriter writer) {
      super.writeContents(writer);

      writer.print("<repRowIdx><![CDATA[" + repRowIdx + "]]></repRowIdx>");
      writer.print("<repColIdx><![CDATA[" + repColIdx + "]]></repColIdx>");
      writer.print("<colHeader><![CDATA[" + colHeader + "]]></colHeader>");
   }

   /**
    * Parse contents.
    * @param elem the specified xml element.
    */
   @Override
   protected void parseContents(Element elem) throws Exception {
      super.parseContents(elem);

      Element node = Tool.getChildNodeByTagName(elem, "repRowIdx");

      if(node != null) {
         repRowIdx = Integer.parseInt(Tool.getValue(node));
      }

      node = Tool.getChildNodeByTagName(elem, "repColIdx");

      if(node != null) {
         repColIdx = Integer.parseInt(Tool.getValue(node));
      }

      node = Tool.getChildNodeByTagName(elem, "colHeader");

      if(node != null) {
         colHeader = Tool.getValue(node);
      }
   }

   public boolean equals(Object obj) {
      if(!(obj instanceof TableCellValue)) {
         return false;
      }

      TableCellValue value = (TableCellValue) obj;

      return super.equals(obj) && repRowIdx == value.repRowIdx && repColIdx == value.repColIdx &&
         Tool.equals(colHeader, value.colHeader);
   }

   private String colHeader;
   private int repRowIdx;
   private int repColIdx = 1;
}