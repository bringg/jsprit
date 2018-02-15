package com.graphhopper.jsprit.core.algorithm.acceptor;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.GreedySchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.listener.AlgorithmStartsListener;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListener;
import com.graphhopper.jsprit.core.algorithm.termination.PrematureAlgorithmTermination;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;

public class SchrimpfInitialThresholdGeneratorTest {
    @Test
    public void informAlgorithmStarts() {
        SchrimpfAcceptance schrimpfAcceptance = mock(SchrimpfAcceptance.class);
        final SchrimpfInitialThresholdGenerator schrimpfInitialThresholdGenerator = new SchrimpfInitialThresholdGenerator(schrimpfAcceptance, 10);

        VehicleTypeImpl type = VehicleTypeImpl.Builder.newInstance("t").setCostPerWaitingTime(1.).build();
        VehicleImpl v = VehicleImpl.Builder.newInstance("v").setType(type).setStartLocation(Location.newInstance(0, 0)).build();
        Service prevS = Service.Builder.newInstance("prev").setLocation(Location.newInstance(10, 0)).build();
        Service newS = Service.Builder.newInstance("new").setServiceTime(10).setLocation(Location.newInstance(60, 0)).build();
        Service nextS = Service.Builder.newInstance("next").setLocation(Location.newInstance(30, 0)).setTimeWindow(TimeWindow.newInstance(40, 80)).build();

        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addJob(prevS).addJob(newS).addJob(nextS).addVehicle(v).build();

        Jsprit.Builder builder = new GreedySchrimpfFactory().createGreedyAlgorithmBuilder(vrp);
        VehicleRoutingAlgorithm vra = builder.buildAlgorithm();

        Listener listener = new Listener();
        vra.getAlgorithmListeners().addListener(listener);
        vra.getAlgorithmListeners().addListener(schrimpfInitialThresholdGenerator);

        Collection<VehicleRoutingProblemSolution> solutions = new HashSet<>();

        schrimpfInitialThresholdGenerator.informAlgorithmStarts(vrp, vra, solutions);
        assertTrue(listener.callsCounter > 0);
    }

    class Listener implements VehicleRoutingAlgorithmListener, AlgorithmStartsListener, PrematureAlgorithmTermination {
        int callsCounter = 0;

        @Override
        public boolean isPrematureBreak(SearchStrategy.DiscoveredSolution discoveredSolution) {
            return false;
        }

        @Override
        public void informAlgorithmStarts(VehicleRoutingProblem problem, VehicleRoutingAlgorithm algorithm, Collection<VehicleRoutingProblemSolution> solutions) {
            callsCounter++;
        }
    }
}
