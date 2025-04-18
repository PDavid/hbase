/*
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
package org.apache.hadoop.hbase.master.balancer;

import static org.apache.hadoop.hbase.master.balancer.HeterogeneousCostRulesTestHelper.DEFAULT_RULES_FILE_NAME;
import static org.apache.hadoop.hbase.master.balancer.HeterogeneousCostRulesTestHelper.createRulesFile;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.HBaseClassTestRule;
import org.apache.hadoop.hbase.HBaseCommonTestingUtil;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.RegionInfo;
import org.apache.hadoop.hbase.client.RegionReplicaUtil;
import org.apache.hadoop.hbase.master.RegionPlan;
import org.apache.hadoop.hbase.testclassification.MasterTests;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ MasterTests.class, MediumTests.class })
public class TestStochasticLoadBalancerHeterogeneousCost extends StochasticBalancerTestBase {

  @ClassRule
  public static final HBaseClassTestRule CLASS_RULE =
    HBaseClassTestRule.forClass(TestStochasticLoadBalancerHeterogeneousCost.class);

  private static final HBaseCommonTestingUtil HTU = new HBaseCommonTestingUtil();
  private static String RULES_FILE;

  @BeforeClass
  public static void beforeAllTests() throws IOException {
    conf = HTU.getConfiguration();
    conf.setFloat("hbase.master.balancer.stochastic.regionCountCost", 0);
    conf.setFloat("hbase.master.balancer.stochastic.primaryRegionCountCost", 0);
    conf.setFloat("hbase.master.balancer.stochastic.tableSkewCost", 0);
    conf.set(StochasticLoadBalancer.COST_FUNCTIONS_COST_FUNCTIONS_KEY,
      HeterogeneousRegionCountCostFunction.class.getName());
    // Need to ensure test dir has been created.
    assertTrue(FileSystem.get(HTU.getConfiguration()).mkdirs(HTU.getDataTestDir()));
    RULES_FILE = HTU.getDataTestDir(DEFAULT_RULES_FILE_NAME).toString();
    conf.set(HeterogeneousRegionCountCostFunction.HBASE_MASTER_BALANCER_HETEROGENEOUS_RULES_FILE,
      RULES_FILE);
    loadBalancer = new StochasticLoadTestBalancer();
    loadBalancer.setClusterInfoProvider(new DummyClusterInfoProvider(conf));
    loadBalancer.initialize();
  }

  @Test
  public void testDefault() throws IOException {
    final List<String> rules = Collections.emptyList();

    final int numNodes = 2;
    final int numRegions = 300;
    final int numRegionsPerServer = 250;

    // Initial state: { rs1:50 , rs0:250 }
    // Cluster can hold 300/400 regions (75%)
    // Expected balanced Cluster: { rs0:150 , rs1:150 }
    this.testHeterogeneousWithCluster(numNodes, numRegions, numRegionsPerServer, rules);
  }

  @Test
  public void testOneGroup() throws IOException {
    final List<String> rules = Collections.singletonList("rs.* 100");

    final int numNodes = 4;
    final int numRegions = 300;
    final int numRegionsPerServer = 30;

    // Initial state: { rs0:30 , rs1:30 , rs2:30 , rs3:210 }.
    // The cluster can hold 300/400 regions (75%)
    // Expected balanced Cluster: { rs0:75 , rs1:75 , rs2:75 , rs3:75 }
    this.testHeterogeneousWithCluster(numNodes, numRegions, numRegionsPerServer, rules);
  }

  @Test
  public void testTwoGroups() throws IOException {
    final List<String> rules = Arrays.asList("rs[0-4] 200", "rs[5-9] 50");

    final int numNodes = 10;
    final int numRegions = 500;
    final int numRegionsPerServer = 50;

    // Initial state: { rs0:50 , rs1:50 , rs2:50 , rs3:50 , rs4:50 , rs5:50 , rs6:50 , rs7:50 ,
    // rs8:50 , rs9:50 }
    // the cluster can hold 500/1250 regions (40%)
    // Expected balanced Cluster: { rs5:20 , rs6:20 , rs7:20 , rs8:20 , rs9:20 , rs0:80 , rs1:80 ,
    // rs2:80 , rs3:80 , rs4:80 }
    this.testHeterogeneousWithCluster(numNodes, numRegions, numRegionsPerServer, rules);
  }

  @Test
  public void testFourGroups() throws IOException {
    final List<String> rules = Arrays.asList("rs[1-3] 200", "rs[4-7] 250", "rs[8-9] 100");

    final int numNodes = 10;
    final int numRegions = 800;
    final int numRegionsPerServer = 80;

    // Initial state: { rs0:80 , rs1:80 , rs2:80 , rs3:80 , rs4:80 , rs5:80 , rs6:80 , rs7:80 ,
    // rs8:80 , rs9:80 }
    // Cluster can hold 800/2000 regions (40%)
    // Expected balanced Cluster: { rs8:40 , rs9:40 , rs2:80 , rs3:80 , rs1:82 , rs0:94 , rs4:96 ,
    // rs5:96 , rs6:96 , rs7:96 }
    this.testHeterogeneousWithCluster(numNodes, numRegions, numRegionsPerServer, rules);
  }

  @Test
  public void testOverloaded() throws IOException {
    final int numNodes = 2;
    final int numRegions = 120;
    final int numRegionsPerServer = 60;

    createRulesFile(RULES_FILE);
    final Map<ServerName, List<RegionInfo>> serverMap =
      this.createServerMap(numNodes, numRegions, numRegionsPerServer, 1, 1);
    final List<RegionPlan> plans =
      loadBalancer.balanceTable(HConstants.ENSEMBLE_TABLE_NAME, serverMap);
    // As we disabled all the other cost functions, balancing only according to
    // the heterogeneous cost function should return nothing.
    assertNull(plans);
  }

  private void testHeterogeneousWithCluster(final int numNodes, final int numRegions,
    final int numRegionsPerServer, final List<String> rules) throws IOException {

    createRulesFile(RULES_FILE, rules);
    final Map<ServerName, List<RegionInfo>> serverMap =
      this.createServerMap(numNodes, numRegions, numRegionsPerServer, 1, 1);
    this.testWithClusterWithIteration(serverMap, null, true, false);
  }

  @Override
  protected Map<ServerName, List<RegionInfo>> createServerMap(int numNodes, int numRegions,
    int numRegionsPerServer, int replication, int numTables) {
    // construct a cluster of numNodes, having a total of numRegions. Each RS will hold
    // numRegionsPerServer many regions except for the last one, which will host all the
    // remaining regions
    int[] cluster = new int[numNodes];
    for (int i = 0; i < numNodes; i++) {
      cluster[i] = numRegionsPerServer;
    }
    cluster[cluster.length - 1] = numRegions - ((cluster.length - 1) * numRegionsPerServer);
    Map<ServerName, List<RegionInfo>> clusterState = mockClusterServers(cluster, numTables);
    if (replication > 0) {
      // replicate the regions to the same servers
      for (List<RegionInfo> regions : clusterState.values()) {
        int length = regions.size();
        for (int i = 0; i < length; i++) {
          for (int r = 1; r < replication; r++) {
            regions.add(RegionReplicaUtil.getRegionInfoForReplica(regions.get(i), r));
          }
        }
      }
    }

    return clusterState;
  }

  @Override
  protected TreeMap<ServerName, List<RegionInfo>> mockClusterServers(int[] mockCluster,
    int numTables) {
    int numServers = mockCluster.length;
    TreeMap<ServerName, List<RegionInfo>> servers = new TreeMap<>();
    for (int i = 0; i < numServers; i++) {
      int numRegions = mockCluster[i];
      ServerAndLoad sal = createServer("rs" + i);
      List<RegionInfo> regions = randomRegions(numRegions, numTables);
      servers.put(sal.getServerName(), regions);
    }
    return servers;
  }

  private Queue<ServerName> serverQueue = new ArrayDeque<>();

  private ServerAndLoad createServer(final String host) {
    if (!this.serverQueue.isEmpty()) {
      ServerName sn = this.serverQueue.poll();
      return new ServerAndLoad(sn, 0);
    }
    Random rand = ThreadLocalRandom.current();
    int port = rand.nextInt(60000);
    long startCode = rand.nextLong();
    ServerName sn = ServerName.valueOf(host, port, startCode);
    return new ServerAndLoad(sn, 0);
  }

  static class FairRandomCandidateGenerator extends RandomCandidateGenerator {

    @Override
    public BalanceAction pickRandomRegions(BalancerClusterState cluster, int thisServer,
      int otherServer) {
      if (thisServer < 0 || otherServer < 0) {
        return BalanceAction.NULL_ACTION;
      }

      int thisRegion = pickRandomRegion(cluster, thisServer, 0.5);
      int otherRegion = pickRandomRegion(cluster, otherServer, 0.5);

      return getAction(thisServer, thisRegion, otherServer, otherRegion);
    }

    @Override
    BalanceAction generate(BalancerClusterState cluster) {
      return super.generate(cluster);
    }
  }

  static class StochasticLoadTestBalancer extends StochasticLoadBalancer {
    private FairRandomCandidateGenerator fairRandomCandidateGenerator =
      new FairRandomCandidateGenerator();

    StochasticLoadTestBalancer() {
      super(new DummyMetricsStochasticBalancer());
    }

    @Override
    protected CandidateGenerator getRandomGenerator(BalancerClusterState cluster) {
      return fairRandomCandidateGenerator;
    }
  }
}
