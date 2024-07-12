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
package inetsoft.web.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.awt.*;
import java.io.IOException;

/**
 * Class that handles serializing {@link Point} objects as JSON.
 *
 * @since 12.3
 */
public class PointSerializer extends StdSerializer<Point> {
   /**
    * Creates a new instance of <tt>PointSerializer</tt>.
    */
   public PointSerializer() {
      super(Point.class);
   }

   @Override
   public void serialize(Point value, JsonGenerator generator,
                         SerializerProvider provider) throws IOException
   {
      generator.writeStartObject();
      generator.writeNumberField("x", value.x);
      generator.writeNumberField("y", value.y);
      generator.writeEndObject();
   }
}
