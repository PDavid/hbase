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
         import="org.apache.hadoop.hbase.io.hfile.BlockCache" %>

<%
  BlockCache bc = (BlockCache) request.getAttribute("bc");

  String bcUrl = bc == null ? null : "http://hbase.apache.org/devapidocs/" + bc.getClass().getName().replaceAll("\\.", "/") + ".html";
  String bcName = bc == null ? null : bc.getClass().getSimpleName();

  BlockCache [] bcs = bc == null? null: bc.getBlockCaches();
  // TODO: These 4 bcl* variables seems not to be used at all!
  String bcl1Url = null;
  String bcl1Name = null;
  String bcl2Url = null;
  String bcl2Name = null;
  if (bcs != null) {
    BlockCache bcl1 = bcs[0];
    if (bcl1 != null) {
      bcl1Url = "http://hbase.apache.org/devapidocs/" + bcl1.getClass().getName().replaceAll("\\.", "/") + ".html";
      bcl1Name = bcl1.getClass().getSimpleName();
    }
    if (bcs.length == 2) {
      BlockCache bcl2 = bcs[1];
      bcl2Url = "http://hbase.apache.org/devapidocs/" + bcl2.getClass().getName().replaceAll("\\.", "/") + ".html";
      bcl2Name = bcl2.getClass().getSimpleName();
    }
  }
%>

<table class="table table-striped">
  <tr>
    <th>Attribute</th>
    <th>Value</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>Implementation</td>
    <td><a href="<%= bcUrl %>"><%= bcName %></a></td>
    <td>Block cache implementing class</td>
  </tr>
</table>
<p>See <a href="http://hbase.apache.org/book.html#block.cache">block cache</a> in the HBase Reference Guide for help.</p>
