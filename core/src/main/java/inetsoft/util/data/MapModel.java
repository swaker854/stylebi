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
package inetsoft.util.data;

import org.springframework.util.CollectionUtils;

import java.util.*;

public class MapModel<T, V> {

   public MapModel() {
      this(null);
   }

   public MapModel(Map<T, V> map) {
      this.mapArray = new ArrayList<>();

      if(CollectionUtils.isEmpty(map)) {
         return;
      }

      for(Map.Entry<T, V> entry : map.entrySet()) {
         this.mapArray.add(new CommonKVModel<>(entry.getKey(), entry.getValue()));
      }
   }

   public List<CommonKVModel<T, V>> getMapArray() {
      return mapArray;
   }

   public void setMapArray(List<CommonKVModel<T, V>> mapArray) {
      this.mapArray = mapArray;
   }

   private List<CommonKVModel<T, V>> mapArray;
}