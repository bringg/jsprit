package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoods;
import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoodsFactory;
import com.graphhopper.jsprit.core.algorithm.ruin.distance.AvgServiceAndShipmentDistance;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GreedyInsertionByDistanceFromDepot extends GreedyInsertion {
    private static Logger logger = LoggerFactory.getLogger(GreedyInsertionByDistanceFromDepot.class);

    private final VehicleFleetManager fleetManager;
    Map<String, Iterator<Job>> jobNeighbors = new HashMap<>();
    Map<VehicleTypeKey, List<Job>> nearestJobByVehicleTypeIdentiffier = new HashMap<>();

    public GreedyInsertionByDistanceFromDepot(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem, VehicleFleetManager fleetManager) {
        super(jobInsertionCalculator, vehicleRoutingProblem);
        this.fleetManager = fleetManager;
        initialize(vehicleRoutingProblem);
    }

    GreedyInsertionByDistanceFromDepot(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem) {
        this(jobInsertionCalculator, vehicleRoutingProblem, new FiniteFleetManagerFactory(vehicleRoutingProblem.getVehicles()).createFleetManager());
    }


    @Override
    public String toString() {
        return "[name=greedyByDistanceFromDepotInsertion]";
    }

    void initialize(VehicleRoutingProblem vehicleRoutingProblem) {
        final VehicleRoutingTransportCosts transportCosts = vehicleRoutingProblem.getTransportCosts();
        JobNeighborhoods neighborhoods = new JobNeighborhoodsFactory().createNeighborhoods(vehicleRoutingProblem, new AvgServiceAndShipmentDistance(transportCosts));
        neighborhoods.initialise();
        for (Job job : vehicleRoutingProblem.getJobs().values()) {
            jobNeighbors.put(job.getId(), neighborhoods.getNearestNeighborsIterator(vehicleRoutingProblem.getJobs().size(), job));

        }
        for (final Vehicle vehicle : vehicleRoutingProblem.getVehicles()) {
            if (nearestJobByVehicleTypeIdentiffier.containsKey(vehicle.getVehicleTypeIdentifier()))
                continue;

            final Comparator<Job> comparator = new Comparator<Job>() {
                @Override
                public int compare(Job job1, Job job2) {
                    double distance1 = transportCosts.getDistance(vehicle.getStartLocation(), getLocation(job1), vehicle.getEarliestDeparture(), vehicle);
                    double distance2 = transportCosts.getDistance(vehicle.getStartLocation(), getLocation(job2), vehicle.getEarliestDeparture(), vehicle);
                    return (int) (distance1 - distance2);
                }
            };
            ArrayList<Job> jobs = new ArrayList<>(vehicleRoutingProblem.getJobs().values());
            Collections.sort(jobs, comparator);
            nearestJobByVehicleTypeIdentiffier.put(vehicle.getVehicleTypeIdentifier(), jobs);
        }
    }

    @Override
    public Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        List<Job> jobsToInsert = new ArrayList<>(unassignedJobs);
        Set<Job> failedToAssign = new HashSet<>(insertBreaks(vehicleRoutes, jobsToInsert));
        List<VehicleRoute> openRoutes = new ArrayList<>(vehicleRoutes);

        while (!jobsToInsert.isEmpty()) {
            if (openRoutes.isEmpty()) {
                List<Vehicle> availableVehicles = new ArrayList<>(fleetManager.getAvailableVehicles());
                if (availableVehicles.isEmpty()) {
                    failedToAssign.addAll(jobsToInsert);
                    return failedToAssign;
                }

                Vehicle nextVehicle = availableVehicles.get(random.nextInt(availableVehicles.size()));
                fleetManager.lock(nextVehicle);
                VehicleRoute newRoute = VehicleRoute.Builder.newInstance(nextVehicle).build();
                openRoutes.add(newRoute);
                vehicleRoutes.add(newRoute);
            }

            VehicleRoute nextRoute = openRoutes.get(random.nextInt(openRoutes.size()));
            Job nearestJob;
            if (nextRoute.isEmpty() || routeWithBreakOnly(nextRoute)) {
                Iterator<Job> nearestJobsIter = nearestJobByVehicleTypeIdentiffier.get(nextRoute.getVehicle().getVehicleTypeIdentifier()).iterator();
                nearestJob = nearestJobsIter.next();
                while (!jobsToInsert.contains(nearestJob) && nearestJobsIter.hasNext()) {
                    nearestJob = nearestJobsIter.next();
                }
            } else {
                List<Job> routeJobs = new ArrayList<>(nextRoute.getTourActivities().getJobs());
                do {
                    nearestJob = routeJobs.get(random.nextInt(routeJobs.size()));
                } while (nearestJob instanceof Break);
            }
            insertJobWithNearest(openRoutes, nextRoute, nearestJob, jobsToInsert);
        }
        return failedToAssign;
    }

    private boolean routeWithBreakOnly(VehicleRoute nextRoute) {
        return nextRoute.getTourActivities().getJobs().size() == 1 && nextRoute.getTourActivities().getJobs().iterator().next() instanceof Break;
    }

    private void insertJobWithNearest(Collection<VehicleRoute> openRoutes, VehicleRoute route, Job jobToInsert, List<Job> jobsToInsert) {
        if (jobsToInsert.contains(jobToInsert)) {
            InsertionData iData = bestInsertionCalculator.getInsertionData(route, jobToInsert, NO_NEW_VEHICLE_YET, NO_NEW_DEPARTURE_TIME_YET, NO_NEW_DRIVER_YET, Double.MAX_VALUE);
            if (!(iData instanceof InsertionData.NoInsertionFound)) {
                super.insertJob(jobToInsert, iData, route);
                jobsToInsert.remove(jobToInsert);
            }
        }

        if (jobNeighbors.containsKey(jobToInsert.getId())) {
            Iterator<Job> jobNeighborsIterator = jobNeighbors.get(jobToInsert.getId());
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
            logger.error("this should not happen route {} jobsThaHavToBeInSameRoute contains key {}", route, jobNeighbors.containsKey(jobToInsert.getId()));
        }
    }
}
