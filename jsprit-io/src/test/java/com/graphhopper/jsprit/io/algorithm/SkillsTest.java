package com.graphhopper.jsprit.io.algorithm;

import com.graphhopper.jsprit.core.algorithm.VariablePlusFixedSolutionCostCalculatorFactory;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkillsTest {
    @Test
    public void skillsAdded() {
        final HashSet<String> first = new HashSet<>(); first.add("C");
        final HashSet<String> second = new HashSet<>(); second.add("A");second.add("B");
        final VehicleRoutingProblem.Builder builder = VehicleRoutingProblem.Builder.newInstance().setFleetSize(VehicleRoutingProblem.FleetSize.FINITE)
            .addVehicle(getVehicle("v1", Location.newInstance(0, 0), 0, 100, 20, first, false, 1, 1))
            .addVehicle(getVehicle("v2", Location.newInstance(0, 14), 0, 100, 20, second, false, 1, 1))
            .addJob(getService(Location.newInstance(0, 5), 0, 20, new HashSet<String>(), 1))
            .addJob(getService(Location.newInstance(0, 6), 0, 20, new HashSet<String>(), 1))
            .addJob(getShipment(Location.newInstance(10, 10), Location.newInstance(10, 12), 10, 20, 10, 50, first, 10));

        builder.setRoutingCost(getTransportCosts());
        VehicleRoutingProblem vrp = builder.build();

        VehicleRoutingProblemSolution bestSolution = getVehicleRoutingProblemSolution(vrp, "com/graphhopper/jsprit/io/algorithm/algorithmConfig.xml");
        assertEquals(bestSolution.getRoutes().size(), 1);
        assertFalse(bestSolution.getUnassignedJobs().isEmpty());

        bestSolution = getVehicleRoutingProblemSolution(vrp, "com/graphhopper/jsprit/io/algorithm/configWithRandomInsert.xml");
        assertEquals(bestSolution.getRoutes().size(), 2);
        assertTrue(bestSolution.getUnassignedJobs().isEmpty());
    }

    private VehicleRoutingProblemSolution getVehicleRoutingProblemSolution(VehicleRoutingProblem vrp, String config) {
        StateManager stateManager = new StateManager(vrp);
        final SolutionCostCalculator calculator = new VariablePlusFixedSolutionCostCalculatorFactory(stateManager).createCalculator();
        SolutionCostCalculator solutionCostCalculator = new SolutionCostCalculator() {
            @Override
            public double getCosts(VehicleRoutingProblemSolution solution) {
                return calculator.getCosts(solution) + solution.getUnassignedJobs().size() * 100;
            }
        };
        final VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, 1, config, stateManager, new ConstraintManager(vrp, stateManager), solutionCostCalculator);
        return Solutions.bestOf(algorithm.searchSolutions());
    }


    private static VehicleRoutingTransportCosts getTransportCosts() {
        return new VehicleRoutingTransportCosts() {
            @Override
            public double getBackwardTransportCost(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) { return getDistance(from, to, arrivalTime, vehicle); }

            @Override
            public double getBackwardTransportTime(Location from, Location to, double arrivalTime, Driver driver, Vehicle vehicle) { return getDistance(from, to, arrivalTime, vehicle); }

            @Override
            public double getTransportCost(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) { return getDistance(from, to, departureTime, vehicle); }

            @Override
            public double getTransportTime(Location from, Location to, double departureTime, Driver driver, Vehicle vehicle) { return getDistance(from, to, departureTime, vehicle); }

            @Override
            public double getDistance(Location from, Location to, double departureTime, Vehicle vehicle) {
                return Math.abs(from.getCoordinate().getX() - to.getCoordinate().getX()) + Math.abs(from.getCoordinate().getY() - to.getCoordinate().getY());
            }
        };
    }

    private static Vehicle getVehicle(String id, Location location, int start, int end, int capacity, Set<String> skills, boolean returnToDepot, int fixedCost, int costPerDistance) {
        return VehicleImpl.Builder.newInstance(id)
            .setStartLocation(location).setLatestArrival(end).setEarliestStart(start).setType(
                VehicleTypeImpl.Builder.newInstance(UUID.randomUUID().toString()).setFixedCost(fixedCost).setCostPerDistance(costPerDistance).addCapacityDimension(0, capacity).build()
            )
            .addAllSkills(skills).setReturnToDepot(returnToDepot).build();

    }

    private static Service getService(Location location, int start, int end, Set<String> requiredSkills, int priority) {
        return Delivery.Builder.newInstance("service_" + UUID.randomUUID().toString().substring(0,5))
            .setLocation(location)
            .setServiceTime(1)
            .addTimeWindow(new TimeWindow(start, end))
            .addSizeDimension(0, 1)
            .addAllRequiredSkills(requiredSkills)
            .setPriority(priority)
            .setName(UUID.randomUUID().toString()).build();

    }

    private static Shipment getShipment(Location pickupLocation, Location dropoffLocation, int pickupStart, int pickupEnd, int dropoffStart, int dropoffEnd, Set<String> skills, int priority) {
        return Shipment.Builder.newInstance("shipment_" + UUID.randomUUID().toString().substring(0,5))
            .setPickupLocation(pickupLocation).setDeliveryLocation(dropoffLocation)
            .setDeliveryServiceTime(1).setPickupServiceTime(1)
            .addSizeDimension(0, 1)
            .setPickupTimeWindow(new TimeWindow(pickupStart, pickupEnd))
            .setDeliveryTimeWindow(new TimeWindow(dropoffStart, dropoffEnd))
            .addAllRequiredSkills(skills)
            .setPriority(priority)
            .build();
    }

}
