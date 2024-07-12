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
package inetsoft.uql.rest.datasource.freshservice;

import inetsoft.uql.rest.auth.AuthType;
import inetsoft.uql.rest.json.EndpointJsonDataSource;
import inetsoft.uql.tabular.*;
import inetsoft.util.Tool;
import org.w3c.dom.Element;

import java.io.PrintWriter;
import java.util.Objects;

@View(vertical = true, value = {
   @View1("domain"),
   @View1("URL"),
   @View1("apiKey")
})
public class FreshserviceDataSource extends EndpointJsonDataSource<FreshserviceDataSource> {
   static final String TYPE = "Rest.Freshservice";
   
   public FreshserviceDataSource() {
      super(TYPE, FreshserviceDataSource.class);
      setAuthType(AuthType.BASIC);
   }

   @Property(label = "Domain", required = true)
   public String getDomain() {
      return domain;
   }

   public void setDomain(String domain) {
      this.domain = domain;
   }

   @Property(label = "API Key", required = true, password = true)
   public String getApiKey() {
      return apiKey;
   }

   public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
   }

   @Property(label = "URL")
   @PropertyEditor(enabled = false, dependsOn = "domain")
   @Override
   public String getURL() {
      StringBuilder url = new StringBuilder("https://");

      if(domain == null) {
         url.append("[Domain]");
      }
      else {
         url.append(domain);
      }

      return url.append(".freshservice.com").toString();
   }

   @Override
   public void setURL(String url) {
      // no-op
   }

   @Override
   public String getUser() {
      return apiKey;
   }

   @Override
   public void setUser(String user) {
      // no-op
   }

   @Override
   public String getPassword() {
      return "X";
   }

   @Override
   public void setPassword(String password) {
      // no-op
   }

   @Override
   public HttpParameter[] getQueryHttpParameters() {
      return new HttpParameter[]{
         HttpParameter.builder()
            .type(HttpParameter.ParameterType.HEADER)
            .name("Content-Type")
            .value("application/json")
            .build()
      };
   }

   @Override
   public void setQueryHttpParameters(HttpParameter[] parameters) {
      // no-op
   }

   @Override
   public void writeContents(PrintWriter writer) {
      super.writeContents(writer);

      if(domain != null) {
         writer.format("<domain><![CDATA[%s]]></domain>%n", domain);
      }

      if(apiKey != null) {
         writer.format("<apiKey><![CDATA[%s]]></apiKey>%n", Tool.encryptPassword(apiKey));
      }
   }

   @Override
   public void parseContents(Element root) throws Exception {
      super.parseContents(root);
      domain = Tool.getChildValueByTagName(root, "domain");
      apiKey = Tool.decryptPassword(Tool.getChildValueByTagName(root, "apiKey"));
   }

   @Override
   protected String getTestSuffix() {
      return "/agents.json";
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) {
         return true;
      }

      if(!(o instanceof FreshserviceDataSource)) {
         return false;
      }

      if(!super.equals(o)) {
         return false;
      }

      FreshserviceDataSource that = (FreshserviceDataSource) o;
      return Objects.equals(domain, that.domain) &&
         Objects.equals(apiKey, that.apiKey);
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), domain, apiKey);
   }

   private String domain;
   private String apiKey;
}