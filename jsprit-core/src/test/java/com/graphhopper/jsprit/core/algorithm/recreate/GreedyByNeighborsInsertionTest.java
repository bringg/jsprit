package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Service;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;

public class GreedyByNeighborsInsertionTest extends TestCase {

    @Test
    public void testJobsInsertedByMostNeighbors() {
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance()
            .addJob(
                Service.Builder.newInstance("1")
                    .setLocation(Location.newInstance(10.1, 10.2))
                    .build()
            )
            .addJob(
                Service.Builder.newInstance("2")
                    .setLocation(Location.newInstance(10.3, 10.1))
                    .build()
            )
            .addJob(
                Service.Builder.newInstance("15")
                    .setLocation(Location.newInstance(-100.5, -100.6))
                    .build()
            )
            .addJob(
                Service.Builder.newInstance("3")
                    .setLocation(Location.newInstance(10.5, 10.6))
                    .build()
            )
            .addJob(
                Service.Builder.newInstance("4")
                    .setLocation(Location.newInstance(-100, -100))
                    .build()
            ).build();
        JobInsertionCostsCalculator jobInsertionCostsCalculator = mock(JobInsertionCostsCalculator.class);
        GreedyByNeighborsInsertion greedyByNeighborsInsertion = new GreedyByNeighborsInsertion(jobInsertionCostsCalculator, vrp, 100);
        Map<String, Integer> neighbors = greedyByNeighborsInsertion.initializeNeighbors();
        assertEquals(2, neighbors.get("1"), 0);
        assertEquals(2, neighbors.get("2"), 0);
        assertEquals(2, neighbors.get("3"), 0);
        assertEquals(1, neighbors.get("4"), 0);
        assertEquals(1, neighbors.get("15"), 0);
    }
}
