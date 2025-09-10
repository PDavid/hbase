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

<%
  HRegionServer regionServer =
    (HRegionServer) getServletContext().getAttribute(HRegionServer.REGIONSERVER);

  BlockCache bc = regionServer.getBlockCache().orElse(null);

  int hitPeriods = 0;
  for (int i = 0; i < bc.getStats().getNumPeriodsInWindow(); i++) {
    if (bc.getStats().getWindowPeriods()[i] != null) {
      hitPeriods++;
    }
  }
%>

<% if (hitPeriods > 0) { %>
<%-- TODO: Extract this JS to separate file! --%>
<script>
  // Wait for document to be fully loaded
  document.addEventListener('DOMContentLoaded', function() {

    // Count actual items in the DOM
    const itemRows = document.querySelectorAll('tr.item-row');

    // Pagination state
    let currentPage = 1;
    const pageSize = 10;
    const totalItems = itemRows.length;
    const totalPages = Math.ceil(totalItems / pageSize);

    // Create page buttons
    const pageButtonsContainer = document.getElementById('page-buttons');
    if (pageButtonsContainer) {
      for (let i = 1; i <= totalPages; i++) {
        const button = document.createElement('button');
        button.className = 'page-number';
        button.textContent = i;
        button.onclick = function() { goToPage(i); };
        pageButtonsContainer.appendChild(button);
      }
    }
    function displayItems() {
      // Hide all item rows
      itemRows.forEach(row => {
        row.style.display = 'none';
      });

      // Calculate indexes
      const startIndex = (currentPage - 1) * pageSize;
      const endIndex = Math.min(startIndex + pageSize, totalItems);

      // Show rows for current page
      let displayedCount = 0;
      for (let i = startIndex; i < endIndex; i++) {
        const row = document.getElementById('row-' + i);
        if (row) {
          row.style.display = 'table-row';
          displayedCount++;
        }
      }

      // Update pagination UI
      document.querySelectorAll('.page-number').forEach(btn => {
        if (parseInt(btn.textContent) === currentPage) {
          btn.classList.add('active');
        } else {
          btn.classList.remove('active');
        }
      });

      const prevBtn = document.getElementById('prev-page');
      const nextBtn = document.getElementById('next-page');

      if (prevBtn) prevBtn.disabled = currentPage === 1;
      if (nextBtn) nextBtn.disabled = currentPage === totalPages;

      // Update page info
      const pageInfo = document.getElementById('page-info');
      if (pageInfo) {
        pageInfo.textContent = `Showing ${startIndex + 1} to ${endIndex} of ${totalItems} items`;
      }
    }

    function goToPage(page) {
      if (page >= 1 && page <= totalPages) {
        currentPage = page;
        displayItems();
      }
    }

    window.nextPage = function() {
      goToPage(currentPage + 1);
    };

    window.prevPage = function() {
      goToPage(currentPage - 1);
    };

    window.goToPage = goToPage;

    // Check URL for initial page
    const urlParams = new URLSearchParams(window.location.search);
    const pageParam = urlParams.get('page');
    if (pageParam) {
      const parsedPage = parseInt(pageParam);
      if (!isNaN(parsedPage) && parsedPage >= 1) {
        currentPage = parsedPage;
      }
    }

    // Initial display
    displayItems();
  });
</script>
<% } %>
<tr>
  <td>Hits</td>
  <td><%= String.format("%,d", bc.getStats().getHitCount()) %></td>
  <td>Number requests that were cache hits</td>
</tr>
<tr>
  <td>Hits Caching</td>
  <td><%= String.format("%,d", bc.getStats().getHitCachingCount()) %></td>
  <td>Cache hit block requests but only requests set to cache block if a miss</td>
</tr>
<tr>
  <td>Misses</td>
  <td><%= String.format("%,d", bc.getStats().getMissCount()) %></td>
  <td>Block requests that were cache misses but set to cache missed blocks</td>
</tr>
<tr>
  <td>Misses Caching</td>
  <td><%= String.format("%,d", bc.getStats().getMissCachingCount()) %></td>
  <td>Block requests that were cache misses but only requests set to use block cache</td>
</tr>
<tr>
  <td>All Time Hit Ratio</td>
  <td><%= String.format("%,.2f", bc.getStats().getHitRatio() * 100) %><%= "%" %></td>
  <td>Hit Count divided by total requests count</td>
</tr>
<% for (int i = 0; i < hitPeriods; i++) { %>
<tr id="row-<%= i %>" class="item-row" style="display: none;">
  <td>Hit Ratio for period starting at <%= bc.getStats().getWindowPeriods()[i] %></td>
  <% if (bc.getStats().getRequestCounts()[i] > 0) { %>
    <td><%= String.format("%,.2f", ((double)bc.getStats().getHitCounts()[i] / (double)bc.getStats().getRequestCounts()[i]) * 100.0) %><%= "%" %></td>
  <% } else { %>
    <td>No requests</td>
  <% } %>
    <td>Hit Count divided by total requests count over the <%= i %>th period of <%= bc.getStats().getPeriodTimeInMinutes() %> minutes</td>
</tr>
<% } %>
<% if (hitPeriods > 0) { %>
  <tr class="pagination-row">
    <td colspan="3">
      <div class="pagination-container">
        <button id="prev-page" onclick="prevPage()">Previous</button>
        <span id="page-buttons" class="page-numbers"></span>
        <button id="next-page" onclick="nextPage()">Next</button>
        <span id="page-info" class="page-info"></span>
      </div>
    </td>
  </tr>
<% } %>
<% if (bc.getStats().getPeriodTimeInMinutes() > 0) { %>
  <tr>
    <td>Last <%= bc.getStats().getNumPeriodsInWindow()*bc.getStats().getPeriodTimeInMinutes() %> minutes Hit Ratio</td>
    <td><%= String.format("%,.2f", bc.getStats().getHitRatioPastNPeriods() * 100.0) %><%= "%" %></td>
    <td>Hit Count divided by total requests count for the last <%= bc.getStats().getNumPeriodsInWindow()*bc.getStats().getPeriodTimeInMinutes() %> minutes</td>
  </tr>
<% } %>
