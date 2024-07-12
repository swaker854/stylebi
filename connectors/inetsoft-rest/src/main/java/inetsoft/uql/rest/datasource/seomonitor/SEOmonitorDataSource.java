/*
 * inetsoft-rest - StyleBI is a business intelligence web application.
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
package inetsoft.uql.rest.datasource.seomonitor;

import inetsoft.uql.rest.json.EndpointJsonDataSource;
import inetsoft.uql.tabular.*;
import inetsoft.util.Tool;
import org.w3c.dom.Element;

import java.io.PrintWriter;
import java.util.Objects;

@View(vertical = true, value = {
   @View1("apiToken")
})
public class SEOmonitorDataSource extends EndpointJsonDataSource<SEOmonitorDataSource> {
   static final String TYPE = "Rest.SEOmonitor";

   public SEOmonitorDataSource() {
      super(TYPE, SEOmonitorDataSource.class);
   }

   @Property(label = "API Token", required = true, password = true)
   public String getApiToken() {
      return apiToken;
   }

   public void setApiToken(String apiToken) {
      this.apiToken = apiToken;
   }

   @Override
   public String getURL() {
      return "https://api.seomonitor.com";
   }

   @Override
   public void setURL(String url) {
      // no-op
   }

   @Override
   public HttpParameter[] getQueryHttpParameters() {
      HttpParameter param = new HttpParameter();
      param.setType(HttpParameter.ParameterType.HEADER);
      param.setName("Authorization");
      param.setValue(apiToken);
      return new HttpParameter[] { param };
   }

   @Override
   public void setQueryHttpParameters(HttpParameter[] parameters) {
      // no-op
   }

   @Override
   public void writeContents(PrintWriter writer) {
      super.writeContents(writer);

      if(apiToken != null) {
         writer.format("<apiToken><![CDATA[%s]]></apiToken>%n", Tool.encryptPassword(apiToken));
      }
   }

   @Override
   public void parseContents(Element root) throws Exception {
      super.parseContents(root);
      apiToken = Tool.decryptPassword(Tool.getChildValueByTagName(root, "apiToken"));
   }

   @Override
   protected String getTestSuffix() {
      return "/v1/sites";
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) {
         return true;
      }

      if(!(o instanceof SEOmonitorDataSource)) {
         return false;
      }

      if(!super.equals(o)) {
         return false;
      }

      SEOmonitorDataSource that = (SEOmonitorDataSource) o;
      return Objects.equals(apiToken, that.apiToken);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), apiToken);
   }

   private String apiToken;
}