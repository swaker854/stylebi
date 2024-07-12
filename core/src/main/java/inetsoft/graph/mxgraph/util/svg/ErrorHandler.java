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
package inetsoft.graph.mxgraph.util.svg;

/**
 * This interface must be implemented and then registred as the error handler
 * in order to be notified of parsing errors.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 */
public interface ErrorHandler {
   /**
    * Called when a parse error occurs.
    */
   void error(ParseException e) throws ParseException;
}