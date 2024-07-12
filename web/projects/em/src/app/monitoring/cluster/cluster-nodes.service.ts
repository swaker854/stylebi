/*
 * inetsoft-web - StyleBI is a business intelligence web application.
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
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ClusterNodesModel } from "./cluster-monitoring-model/cluster-nodes-model";
import { map } from "rxjs/operators";

@Injectable()
export class ClusterNodesService {
   constructor(private http: HttpClient) {
   }

   public getClusterNodesModel(): Observable<ClusterNodesModel> {
      return this.http.get<ClusterNodesModel>("../api/em/cluster/get-cluster-nodes");
   }

   public getClusterNodes(): Observable<string[]> {
      return this.getClusterNodesModel().pipe(
         map((model: ClusterNodesModel) => {
            return model == null ? [] : model.clusterNodes;
         })
      );
   }
}