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
package inetsoft.graph.internal;

import java.io.Serializable;

/**
 * ISize, the object provide common functions for layout.
 *
 * @version 10.0
 * @author InetSoft Technology Corp
 */
public interface ISize extends Cloneable, Serializable {
   /**
    * Get the preferred size of this visualizable.
    */
   public double getPreferredSize();

   /**
    * Get the min size of this visualizable.
    */
   public double getMinSize();

   /**
    * Set the size.
    */
   public void setSize(double val);

   /**
    * Get the size.
    */
   public double getSize();
}
