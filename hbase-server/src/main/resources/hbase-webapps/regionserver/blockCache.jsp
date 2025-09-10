<%--
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
--%>
<%@ page contentType="text/html;charset=UTF-8"
         import="org.apache.hadoop.hbase.io.hfile.BlockCache"
         import="org.apache.hadoop.hbase.regionserver.HRegionServer" %>

<%-- Template for rendering Block Cache tabs in RegionServer Status page. --%>

<%
  HRegionServer regionServer =
    (HRegionServer) getServletContext().getAttribute(HRegionServer.REGIONSERVER);

  BlockCache bc = regionServer.getBlockCache().orElse(null);

  BlockCache[] bcs = bc == null ? null : bc.getBlockCaches();
  // TODO: evictions seems not to be used at all!
  boolean evictions = bcs != null && bcs.length > 1;
  BlockCache l1 = bcs == null ? bc : bcs[0];
  BlockCache l2 = bcs == null ? null : bcs.length <= 1 ? null : bcs[1];
%>

<div class="tabbable">
  <ul class="nav nav-pills" role="tablist">
    <li class="nav-item"><a class="nav-link active" href="#tab_bc_baseInfo" data-bs-toggle="tab" role="tab">Base Info</a></li>
    <li class="nav-item"><a class="nav-link" href="#tab_bc_config" data-bs-toggle="tab" role="tab">Config</a></li>
    <li class="nav-item"><a class="nav-link" href="#tab_bc_stats" data-bs-toggle="tab" role="tab">Stats</a></li>
    <li class="nav-item"><a class="nav-link" href="#tab_bc_l1" data-bs-toggle="tab" role="tab">L1</a></li>
    <li class="nav-item"><a class="nav-link" href="#tab_bc_l2" data-bs-toggle="tab" role="tab">L2</a></li>
  </ul>
  <div class="tab-content">
    <div class="tab-pane active" id="tab_bc_baseInfo" role="tabpanel">
      <jsp:include page="blockCacheBaseInfo.jsp"/>
    </div>
    <div class="tab-pane" id="tab_bc_config" role="tabpanel">
      <jsp:include page="blockCacheConfig.jsp"/>
    </div>
    <div class="tab-pane" id="tab_bc_stats" role="tabpanel">
      <jsp:include page="blockCacheStats.jsp"/>
    </div>
    <div class="tab-pane" id="tab_bc_l1" role="tabpanel">
      <% request.setAttribute("bc", l1); %>
      <% request.setAttribute("name", "L1"); %>
      <% request.setAttribute("evictions", evictions); %>
      <jsp:include page="blockCacheLevel.jsp"/>
    </div>
    <div class="tab-pane" id="tab_bc_l2" role="tabpanel">
      <% request.setAttribute("bc", l2); %>
      <% request.setAttribute("name", "L2"); %>
      <% request.setAttribute("evictions", evictions); %>
      <jsp:include page="blockCacheLevel.jsp"/>
    </div>
  </div>
</div>
