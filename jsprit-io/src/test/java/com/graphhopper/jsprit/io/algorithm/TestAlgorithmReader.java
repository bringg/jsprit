/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.io.algorithm;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.SearchStrategyModule;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.acceptor.GreedyAcceptance;
import com.graphhopper.jsprit.core.algorithm.acceptor.SolutionAcceptor;
import com.graphhopper.jsprit.core.algorithm.listener.IterationEndsListener;
import com.graphhopper.jsprit.core.algorithm.listener.SearchStrategyModuleListener;
import com.graphhopper.jsprit.core.algorithm.ruin.RuinStrategy;
import com.graphhopper.jsprit.core.algorithm.ruin.listener.RuinListener;
import com.graphhopper.jsprit.core.algorithm.selector.SelectBest;
import com.graphhopper.jsprit.core.algorithm.selector.SolutionSelector;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms.ModKey;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms.TypedMap.AcceptorKey;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms.TypedMap.RuinStrategyKey;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms.TypedMap.SelectorKey;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms.TypedMap.StrategyModuleKey;
import com.graphhopper.jsprit.io.problem.VrpXMLReader;
import junit.framework.Assert;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestAlgorithmReader {

    AlgorithmConfig config;

    VehicleRoutingProblem vrp;

    Collection<VehicleRoutingProblemSolution> solutions;

    @Before
    public void doBefore() throws ConfigurationException {
        config = new AlgorithmConfig();
        new AlgorithmConfigXmlReader(config).setSchemaValidation(false).read(getClass().getResource("testConfig.xml"));
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        solutions = new ArrayList<VehicleRoutingProblemSolution>();
        new VrpXMLReader(vrpBuilder, solutions).read(getClass().getResourceAsStream("finiteVrp.xml"));
        vrp = vrpBuilder.build();
    }

    @Test
    public void itShouldReadMaxIterations() {
        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, getClass().getResource("algorithmConfigForReaderTest.xml"));
        Assert.assertEquals(2000, vra.getMaxIterations());
    }

    static class IterationCounter implements IterationEndsListener {

        int iterations = 0;

        @Override
        public void informIterationEnds(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
            iterations = i;
        }

    }

    @Test
    public void whenSettingPrematureBreak_itShouldReadTermination() {
        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, 1, getClass().getResource("algorithmConfigForReaderTest2.xml"));
        IterationCounter iCounter = new IterationCounter();
        vra.addListener(iCounter);
        vra.searchSolutions();
        Assert.assertEquals(100, iCounter.iterations);
    }

    @Test
    public void itShouldReadTermination() {
        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, getClass().getResource("algorithmConfigForReaderTest.xml"));
        IterationCounter iCounter = new IterationCounter();
        vra.addListener(iCounter);
        vra.searchSolutions();
        Assert.assertEquals(25, iCounter.iterations);
    }


    @Test
    public void testTypedMap() {
        VehicleRoutingAlgorithms.TypedMap typedMap = new VehicleRoutingAlgorithms.TypedMap();

        String acceptorName = "acceptor";
        String acceptorId = "acceptorId";

        ModKey key = new ModKey(acceptorName, acceptorId);
        AcceptorKey accKey = new AcceptorKey(key);

        SolutionAcceptor acceptor = new GreedyAcceptance(1);

        typedMap.put(accKey, acceptor);

        assertEquals(acceptor, typedMap.get(accKey));

    }

    @Test
    public void testTypedMap2() {
        VehicleRoutingAlgorithms.TypedMap typedMap = new VehicleRoutingAlgorithms.TypedMap();

        String acceptorName = "acceptor";
        String acceptorId = "acceptorId";

        String selectorName = "selector";
        String selectorId = "selectorId";

        ModKey key = new ModKey(acceptorName, acceptorId);
        AcceptorKey accKey = new AcceptorKey(key);
        SolutionAcceptor acceptor = new GreedyAcceptance(1);

        SelectorKey selKey = new SelectorKey(new ModKey(selectorName, selectorId));
        SolutionSelector selector = new SelectBest();

        typedMap.put(accKey, acceptor);
        typedMap.put(selKey, selector);

        assertEquals(acceptor, typedMap.get(accKey));
        assertEquals(selector, typedMap.get(selKey));
    }

    @Test
    public void testTypedMap3() {
        VehicleRoutingAlgorithms.TypedMap typedMap = new VehicleRoutingAlgorithms.TypedMap();

        String acceptorName = "acceptor";
        String acceptorId = "acceptorId";

        String acceptorName2 = "acceptor2";
        String acceptorId2 = "acceptorId2";

        String selectorName = "selector";
        String selectorId = "selectorId";

        ModKey key = new ModKey(acceptorName, acceptorId);
        AcceptorKey accKey = new AcceptorKey(key);
        SolutionAcceptor acceptor = new GreedyAcceptance(1);

        SelectorKey selKey = new SelectorKey(new ModKey(selectorName, selectorId));
        SolutionSelector selector = new SelectBest();

        AcceptorKey accKey2 = new AcceptorKey(new ModKey(acceptorName2, acceptorId2));
        SolutionAcceptor acceptor2 = new GreedyAcceptance(1);

        typedMap.put(accKey, acceptor);
        typedMap.put(selKey, selector);
        typedMap.put(accKey2, acceptor2);

        assertEquals(acceptor, typedMap.get(accKey));
        assertEquals(selector, typedMap.get(selKey));
        assertEquals(acceptor2, typedMap.get(accKey2));
    }

    @Test
    public void testTypedMap4() {
        VehicleRoutingAlgorithms.TypedMap typedMap = new VehicleRoutingAlgorithms.TypedMap();

        String acceptorName = "acceptor";
        String acceptorId = "acceptorId";

        ModKey key = new ModKey(acceptorName, acceptorId);
        RuinStrategyKey accKey = new RuinStrategyKey(key);
        RuinStrategy acceptor = new RuinStrategy() {

            @Override
            public Collection<Job> ruin(Collection<VehicleRoute> vehicleRoutes) {
                return null;
            }


            @Override
            public void addListener(RuinListener ruinListener) {

            }

            @Override
            public void removeListener(RuinListener ruinListener) {

            }

            @Override
            public Collection<RuinListener> getListeners() {
                return null;
            }

        };

        StrategyModuleKey moduleKey = new StrategyModuleKey(key);
        SearchStrategyModule stratModule = new SearchStrategyModule() {

            @Override
            public VehicleRoutingProblemSolution runAndGetSolution(VehicleRoutingProblemSolution vrpSolution) {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void addModuleListener(
                SearchStrategyModuleListener moduleListener) {

            }
        };

        typedMap.put(accKey, acceptor);
        typedMap.put(moduleKey, stratModule);
        typedMap.put(moduleKey, stratModule);

        assertEquals(acceptor, typedMap.get(accKey));
        assertEquals(stratModule, typedMap.get(moduleKey));

    }

    @Test
    public void initialiseConstructionAlgoCorrectly() {
        VehicleRoutingAlgorithms.createAlgorithm(vrp, config);
        assertTrue(true);
    }

    @Test
    public void whenCreatingAlgorithm_nOfStrategiesIsCorrect() {
        VehicleRoutingAlgorithm algo = VehicleRoutingAlgorithms.createAlgorithm(vrp, config);
        assertEquals(3, algo.getSearchStrategyManager().getStrategies().size());
    }

    @Test
    public void whenCreatingAlgorithm_nOfIterationsIsReadCorrectly() {
        VehicleRoutingAlgorithm algo = VehicleRoutingAlgorithms.createAlgorithm(vrp, config);
        assertEquals(10, algo.getMaxIterations());
    }

    @Test
    public void whenCreatingAlgorithm_nOfStrategyModulesIsCorrect() {
        VehicleRoutingAlgorithm algo = VehicleRoutingAlgorithms.createAlgorithm(vrp, config);
        int nOfModules = 0;
        for (SearchStrategy strat : algo.getSearchStrategyManager().getStrategies()) {
            nOfModules += strat.getSearchStrategyModules().size();
        }
        assertEquals(3, nOfModules);
    }

    @Test
    public void readerTest_whenReadingAlgoWithSchemaValidation_itReadsCorrectly() {
        AlgorithmConfig algoConfig = new AlgorithmConfig();
        new AlgorithmConfigXmlReader(algoConfig).read(getClass().getResource("algorithmConfig.xml"));

    }

    @Test
    public void readerTest_whenReadingAlgoWithSchemaValidationWithoutIterations_itReadsCorrectly() {
        AlgorithmConfig algoConfig = new AlgorithmConfig();
        new AlgorithmConfigXmlReader(algoConfig).read(getClass().getResource("algorithmConfig_withoutIterations.xml"));

    }

    @Test
    public void testReaderWithRightUseOfHints() {
        Random random = new Random();
        int serviceTime = random.nextInt(5) + 1;
        double serviceTimeOnSameLocation = random.nextDouble();
        List<TourActivity> activities = testIt(serviceTime, serviceTimeOnSameLocation, true);

        TourActivity activity1 = activities.get(0);
        TourActivity activity2 = activities.get(1);
        TourActivity activity3 = activities.get(2);
        TourActivity activity4 = activities.get(3);

        assertTrue(activity1.getLocation().getCoordinate().equals(activity2.getLocation().getCoordinate()));
        assertFalse(activity3.getLocation().getCoordinate().equals(activity4.getLocation().getCoordinate()));

        assertTrue(assertDoubleEquals(activity1.getEndTime() - activity1.getArrTime(), serviceTime));
        assertTrue(assertDoubleEquals(activity2.getEndTime() - activity2.getArrTime(), serviceTimeOnSameLocation));
        assertTrue(assertDoubleEquals(activity3.getEndTime() - activity3.getArrTime(), serviceTime));
        assertTrue(assertDoubleEquals(activity4.getEndTime() - activity4.getArrTime(), serviceTime));
    }

    @Test
    public void testReaderWithRightUseOfHintsSqashFalse() {
        Random random = new Random();
        double serviceTimeOnSameLocation = random.nextDouble();
        int serviceTime = random.nextInt(5);
        List<TourActivity> activities = testIt(serviceTime, serviceTimeOnSameLocation, false);

        TourActivity activity1 = activities.get(0);
        TourActivity activity2 = activities.get(1);
        TourActivity activity3 = activities.get(2);
        TourActivity activity4 = activities.get(3);

        assertTrue(activity1.getLocation().getCoordinate().equals(activity2.getLocation().getCoordinate()));
        assertFalse(activity3.getLocation().getCoordinate().equals(activity4.getLocation().getCoordinate()));

        assertTrue(assertDoubleEquals(activity1.getEndTime() - activity1.getArrTime(), serviceTime));
        assertTrue(assertDoubleEquals(activity2.getEndTime() - activity2.getArrTime(), serviceTime));
        assertTrue(assertDoubleEquals(activity3.getEndTime() - activity3.getArrTime(), serviceTime));
        assertTrue(assertDoubleEquals(activity4.getEndTime() - activity4.getArrTime(), serviceTime));
    }

    private boolean assertDoubleEquals(double x1, double x2) {
        return Math.abs(x1 - x2) < 1E-10;
    }

    private List<TourActivity> testIt(int serviceTime, double serviceTimeOnSameLocation, boolean sqash) {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<problem xmlns=\"http://www.w3schools.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.w3schools.com vrp_xml_schema.xsd\">\n" +
            "  <problemType>\n" +
            "    <fleetSize>FINITE</fleetSize>\n" +
            "    <fleetComposition>HOMOGENEOUS</fleetComposition>\n" +
            "  </problemType>\n" +
            "  <vehicles>\n" +
            "    <vehicle>\n" +
            "      <id>57145</id>\n" +
            "      <typeId>748</typeId>\n" +
            "      <startLocation>\n" +
            "        <id>home_57145</id>\n" +
            "        <coord x=\"" + 34.840348 + "\" y=\"" + 32.114538 + "\"/>\n" +
            "      </startLocation>\n" +
            "      <timeSchedule>\n" +
            "        <start>0</start>\n" +
            "        <end>480</end>\n" +
            "      </timeSchedule>\n" +
            "      <returnToDepot>false</returnToDepot>\n" +
            "    </vehicle>\n" +
            "    <vehicle>\n" +
            "      <id>57146</id>\n" +
            "      <typeId>748</typeId>\n" +
            "      <startLocation>\n" +
            "        <id>home_57145</id>\n" +
            "        <coord x=\"" + 34.840348 + "\" y=\"" + 32.114538 + "\"/>\n" +
            "      </startLocation>\n" +
            "      <timeSchedule>\n" +
            "        <start>0</start>\n" +
            "        <end>480</end>\n" +
            "      </timeSchedule>\n" +
            "      <returnToDepot>false</returnToDepot>\n" +
            "    </vehicle>\n" +
            "  </vehicles>\n" +
            "  <vehicleTypes>\n" +
            "    <type>\n" +
            "      <id>748</id>\n" +
            "      <capacity>60</capacity>\n" +
            "      <costs>\n" +
            "        <fixed>0</fixed>\n" +
            "        <distance>1</distance>\n" +
            "        <time>0</time>\n" +
            "      </costs>\n" +
            "    </type>\n" +
            "  </vehicleTypes>\n" +
            "  <services/>\n" +
            "  <shipments>\n" +
            "    <shipment id=\"1\">\n" +
            "      <pickup>\n" +
            "        <locationId>6550849_8390421</locationId>\n" +
            "        <coord x=\"" + 34.834159 + "\" y=\"" + 32.106582 + "\"/>\n" +
            "        <duration>" + serviceTime + "</duration>\n" +
            "        <timeWindows>\n" +
            "          <timeWindow>\n" +
            "            <start>0</start>\n" +
            "            <end>480</end>\n" +
            "          </timeWindow>\n" +
            "        </timeWindows>\n" +
            "      </pickup>\n" +
            "      <delivery>\n" +
            "        <locationId>6550850_8390422</locationId>\n" +
            "        <coord x=\"" + 34.818851 + "\" y=\"" + 32.105050 + "\"/>\n" +
            "        <duration>" + serviceTime + "</duration>\n" +
            "        <timeWindows>\n" +
            "          <timeWindow>\n" +
            "            <start>0</start>\n" +
            "            <end>480</end>\n" +
            "          </timeWindow>\n" +
            "        </timeWindows>\n" +
            "      </delivery>\n" +
            "      <capacity-demand>1</capacity-demand>\n" +
            "    </shipment>\n" +
            "    <shipment id=\"2\">\n" +
            "      <pickup>\n" +
            "        <locationId>6550851_8390423</locationId>\n" +
            "        <coord x=\"" + 34.834159 + "\" y=\"" + 32.106582 + "\"/>\n" +
            "        <duration>" + serviceTime + "</duration>\n" +
            "        <timeWindows>\n" +
            "          <timeWindow>\n" +
            "            <start>0</start>\n" +
            "            <end>480</end>\n" +
            "          </timeWindow>\n" +
            "        </timeWindows>\n" +
            "      </pickup>\n" +
            "      <delivery>\n" +
            "        <locationId>6550852_8390424</locationId>\n" +
            "        <coord x=\"" + 34.824272 + "\" y=\"" + 32.088730 + "\"/>\n" +
            "        <duration>" + serviceTime + "</duration>\n" +
            "        <timeWindows>\n" +
            "          <timeWindow>\n" +
            "            <start>0</start>\n" +
            "            <end>480</end>\n" +
            "          </timeWindow>\n" +
            "        </timeWindows>\n" +
            "      </delivery>\n" +
            "      <capacity-demand>1</capacity-demand>\n" +
            "    </shipment>\n" +
            "  </shipments>\n" +
            "</problem>";

        final VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        final InputStream requestInputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        new VrpXMLReader(vrpBuilder).read(requestInputStream);

        Map<String, Object> hints = new HashMap<>();
        hints.put("sqash", sqash);
        hints.put("time_on_same_location", serviceTimeOnSameLocation);
        VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrpBuilder.build(), Runtime.getRuntime().availableProcessors() + 1, getClass().getResource("algorithmConfigForReaderTest.xml"), hints);

        VehicleRoutingProblemSolution vehicleRoutingProblemSolution = Solutions.bestOf(vra.searchSolutions());
        assertTrue(vehicleRoutingProblemSolution.getUnassignedJobs().isEmpty());
        return vehicleRoutingProblemSolution.getRoutes().iterator().next().getActivities();
    }
}
