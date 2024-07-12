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
package inetsoft.web.portal.model;

import inetsoft.sree.RepletFolderEntry;
import org.springframework.stereotype.Component;

public class RepletFolderEntryModel extends DefaultFolderEntryModel<RepletFolderEntry> {
   public RepletFolderEntryModel() {
   }

   public RepletFolderEntryModel(RepletFolderEntry entry) {
      super(entry);
      setClassType("RepletFolderEntry");
   }

   @Override
   public RepletFolderEntry createRepositoryEntry() {
      RepletFolderEntry entry = new RepletFolderEntry();
      setProperties(entry);
      return entry;
   }


   @Component
   public static final class RepletFolderEntryModelFactory
      extends RepositoryEntryModelFactory<RepletFolderEntry, RepletFolderEntryModel>
   {
      public RepletFolderEntryModelFactory() {
         super(RepletFolderEntry.class);
      }

      @Override
      public RepletFolderEntryModel createModel(RepletFolderEntry entry) {
         return new RepletFolderEntryModel(entry);
      }
   }
}