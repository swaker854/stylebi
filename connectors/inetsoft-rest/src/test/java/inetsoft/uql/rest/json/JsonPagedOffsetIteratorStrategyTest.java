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
package inetsoft.uql.rest.json;

import inetsoft.test.RequestResponse;
import inetsoft.test.TestHttpHandler;
import inetsoft.test.TestHttpResponse;
import inetsoft.uql.rest.RestErrorHandler;
import inetsoft.uql.rest.RestRequest;
import inetsoft.uql.rest.pagination.PaginationParamType;
import inetsoft.uql.rest.pagination.PaginationParameter;
import inetsoft.uql.rest.pagination.PaginationSpec;
import inetsoft.uql.rest.pagination.PaginationType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonPagedOffsetIteratorStrategyTest {
    @Test
    public void test() throws Exception {
        final RestJsonQuery query = new RestJsonQuery();
        final RestJsonDataSource datasource = new RestJsonDataSource();
        datasource.setURL("http://host");
        query.setDataSource(datasource);
        final PaginationSpec spec = query.getPaginationSpec();
        spec.setType(PaginationType.OFFSET);
        spec.setRecordCountPath("$.results");
        spec.setOffsetParam(new PaginationParameter("offset", PaginationParamType.QUERY));
        spec.setMaxResultsPerPage(10);
        spec.setZeroBasedPageIndex(false);

        final List<RequestResponse> requestResponses = new ArrayList<>();

        final String page1 = "{\"results\": 2}";
        final String page2 = "{\"results\": 3}";
        final String page3 = "{}";

        requestResponses.add(new RequestResponse(RestRequest.builder().query(query)
           .queryParameters(Collections.singletonMap("offset", "1"))
           .build(), new TestHttpResponse(page1)));
        requestResponses.add(new RequestResponse(RestRequest.builder().query(query)
           .queryParameters(Collections.singletonMap("offset", "3"))
           .build(), new TestHttpResponse(page2)));
        requestResponses.add(new RequestResponse(RestRequest.builder().query(query)
           .queryParameters(Collections.singletonMap("offset", "6"))
           .build(), new TestHttpResponse(page3)));

        final TestHttpHandler httpHandler = new TestHttpHandler(requestResponses);
        final JsonTransformer transformer = new JsonTransformer();

        final JsonPagedOffsetIteratorStrategy strategy = new JsonPagedOffsetIteratorStrategy(
           query, transformer, httpHandler, new RestErrorHandler());
        int count = 0;

        while(strategy.hasNext()) {
            assertNotNull(strategy.next());
            count++;
        }

        assertEquals(2, count);
    }
}