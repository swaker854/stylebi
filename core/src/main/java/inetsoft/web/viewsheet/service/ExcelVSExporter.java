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
package inetsoft.web.viewsheet.service;

import inetsoft.report.io.viewsheet.AbstractVSExporter;
import inetsoft.uql.viewsheet.FileFormatInfo;

/**
 * Class used to export Excel files.
 */
public abstract class ExcelVSExporter extends AbstractVSExporter {
   @Override
   public int getFileFormatType() {
      return FileFormatInfo.EXPORT_TYPE_EXCEL;
   }

   public abstract void setExcelToCSV(boolean excel);

   public void setExportAllTabbedTables(boolean exportAllTabbedTables) {
      this.exportAllTabbedTables = exportAllTabbedTables;
   }

   public boolean isExportAllTabbedTables() {
      return exportAllTabbedTables;
   }

   private boolean exportAllTabbedTables;
}
