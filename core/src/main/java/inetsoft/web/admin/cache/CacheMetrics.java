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
package inetsoft.web.admin.cache;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
public abstract class CacheMetrics {
   @Value.Default
   public int dataMemoryCount() {
      return 0;
   }

   @Value.Default
   public int dataDiskCount() {
      return 0;
   }

   @Value.Default
   public long dataBytesRead() {
      return 0L;
   }

   @Value.Default
   public long dataBytesWritten() {
      return 0L;
   }

   @Value.Default
   public int dataHits() {
      return 0;
   }

   @Value.Default
   public int dataMisses() {
      return 0;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder extends ImmutableCacheMetrics.Builder {
   }
}
