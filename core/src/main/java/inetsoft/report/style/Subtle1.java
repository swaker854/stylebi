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
package inetsoft.report.style;

import inetsoft.report.TableLens;

import java.awt.*;

/**
 * The Subtle1 class is a table style class. It can be used with other
 * table lens classes to provide a visual style for table formatting and
 * printing. The default style for Subtle1 is:<p>
 * <img src="images/Subtle1.gif"><p>
 * Additional formatting for the last row and column may be available.
 * Please check the style guide or use StyleViewer to experiment with
 * the different settings of the style.
 *
 * @version 5.1, 9/20/2003
 * @auther InetSoft Technology Corp
 */
public class Subtle1 extends TableStyle {
   /**
    * Create an empty style. The setTable() method must be called before
    * it can be used.
    */
   public Subtle1() {
   }

   /**
    * Create a style to decorate the specified table.
    * @param table table lens.
    */
   public Subtle1(TableLens table) {
      super(table);
   }

   /**
    * Create a style to decorate the table.
    * @return a style lens.
    */
   @Override
   protected TableLens createStyle(TableLens tbl) {
      return new Style();
   }

   /**
    * Style lens.
    */
   class Style extends Transparent {
      /**
       * Return the style for bottom border of the specified cell. The flag
       * must be one of the style options defined in the StyleConstants
       * class. If the row number is -1, it's checking the outside ruling
       * on the top.
       * @param r row number.
       * @param c column number.
       * @return ruling flag.
       */
      @Override
      public int getRowBorder(int r, int c) {
         if(isHeaderRowFormat(r) && !isHeaderRowFormat(r + 1) ||
            isTrailerRowFormat(r + 1) && !isTrailerRowFormat(r)) {
            return MEDIUM_LINE;
         }

         return NO_BORDER;
      }

      /**
       * Return the style for right border of the specified row. The flag
       * must be one of the style options defined in the StyleConstants
       * class. If the column number is -1, it's checking the outside ruling
       * on the left.
       * @param r row number.
       * @param c column number.
       * @return ruling flag.
       */
      @Override
      public int getColBorder(int r, int c) {
         if(isHeaderColFormat(c) && !isHeaderColFormat(c + 1) ||
            isTrailerColFormat(c + 1) && !isTrailerColFormat(c)) {
            return MEDIUM_LINE;
         }

         return NO_BORDER;
      }

      /**
       * Return the per cell alignment.
       * @param r row number.
       * @param c column number.
       * @return cell alignment.
       */
      @Override
      public int getAlignment(int r, int c) {
         return (H_LEFT | V_CENTER);
      }

      /**
       * Return the per cell font. Return null to use default font.
       * @param r row number.
       * @param c column number.
       * @return font for the specified cell.
       */
      @Override
      public Font getFont(int r, int c) {
         Font font = table.getFont(r, c);

         return (isHeaderRowFormat(r) || isTrailerColFormat(c) ||
            isHeaderColFormat(c) || isTrailerRowFormat(r)) ?
            createFont(font, Font.BOLD) :
            font;
      }

      /**
       * Return the per cell background color. Return null to use default
       * color.
       * @param r row number.
       * @param c column number.
       * @return background color for the specified cell.
       */
      @Override
      public Color getBackground(int r, int c) {
         if(isTrailerRowFormat(r)) {
            return new Color(255, 128, 128);
         }

         return (r % 2 == 1) ? new Color(255, 255, 200) : null;
      }
   }
}
