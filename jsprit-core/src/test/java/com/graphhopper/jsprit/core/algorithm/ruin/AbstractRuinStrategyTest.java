package com.graphhopper.jsprit.core.algorithm.ruin;

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

import static java.util.Collections.EMPTY_LIST;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractRuinStrategyTest {
    @Test
    public void addAllRoutesBreaksToUnassignedJobs() {
        test(1 + Math.abs(new Random().nextInt(100)));
    }

    @Test
    public void noBreaksInUnassignedJobs() {
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().build();
        final AbstractRuinStrategy abstractRuinStrategy = new AbstractRuinStrategy(vrp) {
            @Override
            public Collection<Job> ruinRoutes(Collection<VehicleRoute> vehicleRoutes) { return EMPTY_LIST; }
        };

        Collection<VehicleRoute> vehicleRoutes = new ArrayList<>();
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getBreak()).thenReturn(null);
        vehicleRoutes.add(VehicleRoute.Builder.newInstance(vehicle).build());

        assertEquals(abstractRuinStrategy.ruin(vehicleRoutes).size(), 0);
    }

    private void test(int numRoutes) {
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().build();
        final AbstractRuinStrategy abstractRuinStrategy = new AbstractRuinStrategy(vrp) {
            @Override
            public Collection<Job> ruinRoutes(Collection<VehicleRoute> vehicleRoutes) { return EMPTY_LIST; }
        };

        Collection<VehicleRoute> vehicleRoutes = new ArrayList<>();
        for (int i = 0; i < numRoutes; ++i) {
            Vehicle vehicle = mock(Vehicle.class);
            when(vehicle.getBreak()).thenReturn(Break.Builder.newInstance(UUID.randomUUID().toString()).build());
            vehicleRoutes.add(VehicleRoute.Builder.newInstance(vehicle).build());
        }
        assertEquals(abstractRuinStrategy.ruin(vehicleRoutes).size(), numRoutes);
    }
}
