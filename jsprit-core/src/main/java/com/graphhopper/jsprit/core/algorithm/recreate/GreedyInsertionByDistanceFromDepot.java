package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoods;
import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoodsFactory;
import com.graphhopper.jsprit.core.algorithm.ruin.distance.AvgServiceAndShipmentDistance;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.DriverImpl;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GreedyInsertionByDistanceFromDepot extends GreedyInsertion {
    private static Logger logger = LoggerFactory.getLogger(GreedyInsertionByDistanceFromDepot.class);

    Map<String, Double> distanceFromDepot = new HashMap<>();
    private final JobInsertionCostsCalculator bestInsertionCalculator;
    Map<String, Iterator<Job>> jobNeighbors = new HashMap<>();

    Comparator<Job> nearestToDepotComparator = new Comparator<Job>() {
        @Override
        public int compare(Job job1, Job job2) {
            double distanceToJob1 = distanceFromDepot.containsKey(job1.getId()) ? distanceFromDepot.get(job1.getId()) : 0;
            double distanceToJob2 = distanceFromDepot.containsKey(job2.getId()) ? distanceFromDepot.get(job2.getId()) : 0;
            return (int) (distanceToJob1 - distanceToJob2);
        }
    };

    public GreedyInsertionByDistanceFromDepot(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem) {
        super(jobInsertionCalculator, vehicleRoutingProblem);
        this.bestInsertionCalculator = jobInsertionCalculator;
        initialize(vehicleRoutingProblem);
    }


    @Override
    public String toString() {
        return "[name=greedyByDistanceFromDepotInsertion]";
    }

    void initialize(VehicleRoutingProblem vehicleRoutingProblem) {
        Location depot;
        double startTime = 0;
        Vehicle vehicle = VehicleImpl.createNoVehicle();
        if (!vehicleRoutingProblem.getVehicles().isEmpty()) {
            vehicle = vehicleRoutingProblem.getVehicles().iterator().next();
            startTime = vehicle.getEarliestDeparture();
            depot = vehicle.isReturnToDepot() ? vehicle.getEndLocation() : vehicle.getStartLocation();
        } else {
            depot = getLocation(vehicleRoutingProblem.getJobsInclusiveInitialJobsInRoutes().values().iterator().next());
        }

        VehicleRoutingTransportCosts transportCosts = vehicleRoutingProblem.getTransportCosts();
        JobNeighborhoods neighborhoods = new JobNeighborhoodsFactory().createNeighborhoods(vehicleRoutingProblem, new AvgServiceAndShipmentDistance(transportCosts));
        neighborhoods.initialise();
        for (Job job : vehicleRoutingProblem.getJobs().values()) {
            distanceFromDepot.put(job.getId(), transportCosts.getTransportTime(depot, getLocation(job), startTime, DriverImpl.noDriver(), vehicle));
            jobNeighbors.put(job.getId(), neighborhoods.getNearestNeighborsIterator(vehicleRoutingProblem.getJobs().size(), job));
        }
    }

    @Override
    public Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        Set<Job> failedToAssign = new HashSet<>();
        List<Job> jobsToInsert = new ArrayList<>(unassignedJobs);
        List<VehicleRoute> openRoutes = new ArrayList<>(vehicleRoutes);

        Collections.sort(jobsToInsert, nearestToDepotComparator);
        while (!jobsToInsert.isEmpty()) {
            Job withMostNeighbors = jobsToInsert.remove(0);
            failedToAssign.addAll(insertJobWithNearest(vehicleRoutes, openRoutes, withMostNeighbors, jobsToInsert));
        }
        return failedToAssign;
    }

    private Collection<Job> insertJobWithNearest(Collection<VehicleRoute> vehicleRoutes, Collection<VehicleRoute> openRoutes, Job withMostNeighbors, List<Job> jobsToInsert) {
        List<Job> jobs = new ArrayList<>();
        jobs.add(withMostNeighbors);
        Collection<Job> failedToInsert = super.insertUnassignedJobs(openRoutes, jobs);
        if (!failedToInsert.isEmpty())
            return failedToInsert;

        VehicleRoute route = findRoute(openRoutes, withMostNeighbors);
        if (!vehicleRoutes.contains(route))
            vehicleRoutes.add(route);

        if (route != null && jobNeighbors.containsKey(withMostNeighbors.getId())) {
            Iterator<Job> jobNeighborsIterator = jobNeighbors.get(withMostNeighbors.getId());
            while (jobNeighborsIterator.hasNext()) {
                Job job = jobNeighborsIterator.next();
                if (jobsToInsert.contains(job)) {
                    InsertionData iData = bestInsertionCalculator.getInsertionData(route, job, NO_NEW_VEHICLE_YET, NO_NEW_DEPARTURE_TIME_YET, NO_NEW_DRIVER_YET, Double.MAX_VALUE);
                    if (!(iData instanceof InsertionData.NoInsertionFound)) {
                        super.insertJob(job, iData, route);
                        jobsToInsert.remove(job);
                    }
                }
            }
            openRoutes.remove(route);
        } else {
            logger.error("this should not happen route {} jobsThaHavToBeInSameRoute contains key {}", route, jobNeighbors.containsKey(withMostNeighbors.getId()));
        }

        return failedToInsert;
    }
}
