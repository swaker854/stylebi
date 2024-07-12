/*
 * This file is part of StyleBI.
 *
 * Copyright (c) 2024, InetSoft Technology Corp, All Rights Reserved.
 *
 * The software and information contained herein are copyrighted and
 * proprietary to InetSoft Technology Corp. This software is furnished
 * pursuant to a written license agreement and may be used, copied,
 * transmitted, and stored only in accordance with the terms of such
 * license and with the inclusion of the above copyright notice. Please
 * refer to the file "COPYRIGHT" for further copyright and licensing
 * information. This software and information or any other copies
 * thereof may not be provided or otherwise made available to any other
 * person.
 */
package inetsoft.report.io.viewsheet.excel.chart;

import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;
import java.util.List;

/**
 * Represents DrawingML scatter charts.
 *
 * @version 12.2, 7/19/2015
 * @author InetSoft Technology Corp
 */
public class XSSFScatterChartData extends XSSFChartData {
   /**
    * Constructor, create a XSSFScatterChartData.
    * @param style current chart style.
    */
   public XSSFScatterChartData(XSSFChart chart, int style) {
      super(chart, style);
   }

   @Override
   public Serie addSeries(XDDFDataSource<?> category,
                          XDDFNumericalDataSource<? extends Number> values,
                          SerieInfo sinfo)
   {
      int size = series.size();
      return addSeries(size, size, sinfo, category, values);
   }

   /**
    * @param id the serie id.
    * @param order the serie order.
    * @param sinfo the serie info.
    * @param category the categories datasource
    * @param values the values datasource
    */
   public Serie addSeries(int id, int order, SerieInfo sinfo,
                          XDDFDataSource<?> category,
                          XDDFNumericalDataSource<? extends Number> values)
   {
      Serie added = new Serie(id, order, sinfo, category, values);
      series.add(added);
      return added;
   }

   @Override
   public void fillChart(XDDFChartAxis category, XDDFValueAxis values) {
      XSSFChart xssfChart = getChart();
      CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();
      CTScatterChart scatterChart = plotArea.addNewScatterChart();
      scatterChart.setVaryColors(XSSFChartUtil.getVaryColor());

      CTScatterStyle scatterStyle = scatterChart.addNewScatterStyle();
      scatterStyle.setVal(STScatterStyle.MARKER);

      for(Series s : series) {
         ((Serie) s).addToChart(scatterChart);
      }

      scatterChart.addNewAxId().setVal(category.getId());
      scatterChart.addNewAxId().setVal(values.getId());
   }

   class Serie extends AbstractXSSFChartSerie0 {
      /**
       * Constructor, create a new Serie for CTLineChart.
       * @param id the serie id.
       * @param order the serie order.
       * @param sinfo the serie info.
       * @param xs the categories datasource
       * @param ys the values datasource
       */
      protected Serie(int id, int order, SerieInfo sinfo,
                      XDDFDataSource<?> xs,
                      XDDFNumericalDataSource<? extends Number> ys)
      {
         super(xs, ys);
         this.id = id;
         this.order = order;
         this.serieInfo = sinfo;
         this.xs = xs;
         this.ys = ys;
      }

      /**
       * Add current serie to target scatterchart.
       */
      protected void addToChart(CTScatterChart ctScatterChart) {
         CTScatterSer scatterSer = ctScatterChart.addNewSer();
         this.series = scatterSer;
         scatterSer.addNewIdx().setVal(this.id);
         scatterSer.addNewOrder().setVal(this.order);

         CTAxDataSource xVal = scatterSer.addNewXVal();
         XSSFChartUtil.buildAxDataSource(xVal, xs, true);

         CTNumDataSource yVal = scatterSer.addNewYVal();
         XSSFChartUtil.buildNumDataSource(yVal, ys);

         if(serieInfo != null) {
            scatterSer.addNewTx();
            setTitle(serieInfo.getTitle(), null);
            scatterSer.setTx(getSeriesText());

            // fix Bug #5467, add default shape properties and
            // maker properties to make sure the legend display
            // right marker and color.
            CTShapeProperties spPr = getDefaultShapeProperties();

            if(spPr != null) {
               scatterSer.setSpPr(spPr);
            }

            CTMarker marker = getDefaultMarker();

            if(marker != null) {
               scatterSer.setMarker(marker);
            }

            if(serieInfo.isShowValue()) {
               scatterSer.setDLbls(getDataLabelsProperties());
            }

            CTDPt[] dPts = getDataPointsProperties();

            if(dPts != null) {
               scatterSer.setDPtArray(dPts);
            }

            int trendline = serieInfo.getTrendline();

            if(trendline > 0) {
               CTTrendline ctTreandLine = scatterSer.addNewTrendline();
               Color trendLineColor = serieInfo.getTrendLineColor();
               int trendLineStyle = serieInfo.getTrendLineStyle();
               XSSFChartUtil.propertyTrendLine(ctTreandLine, trendline,
                                               trendLineColor, trendLineStyle);
            }
         }
      }

      @Override
      protected CTAxDataSource getAxDS() {
         return series.getXVal();
      }

      @Override
      protected CTNumDataSource getNumDS() {
         return series.getYVal();
      }

      @Override
      protected CTSerTx getSeriesText() {
         return series.getTx();
      }

      @Override
      protected List<CTDPt> getDPtList() {
         return series.getDPtList();
      }

      private CTScatterSer series;

      private final XDDFDataSource<?> xs;
      private final XDDFNumericalDataSource<? extends Number> ys;
   }
}
