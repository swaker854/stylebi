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
package inetsoft.storage;

import inetsoft.sree.internal.cluster.SingletonRunnableTask;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

/**
 * {@code PutAllKeyValueTask} is cluster singleton task that bulk inserts a set of key-value pairs.
 *
 * @param <T> the value type.
 */
public class PutAllKeyValueTask<T extends Serializable>
   extends KeyValueTask<T> implements SingletonRunnableTask
{
   /**
    * Creates a new instance of {@code PutAllKeyValueTask}.
    *
    * @param id     the unique identifier of the key-value store.
    * @param values the new values.
    */
   public PutAllKeyValueTask(String id, SortedMap<String, T> values) {
      super(id);
      this.data = serializeValue((Serializable) values);
   }

   @Override
   public void run() {
      SortedMap<String, T> values = deserializeValue(data);

      for(Map.Entry<String, T> e : values.entrySet()) {
         getEngine().put(getId(), e.getKey(), e.getValue());
      }

      getMap().putAll(values);
   }

   private final byte[] data;
}
