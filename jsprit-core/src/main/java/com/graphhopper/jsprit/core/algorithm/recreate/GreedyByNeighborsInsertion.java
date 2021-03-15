package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoods;
import com.graphhopper.jsprit.core.algorithm.ruin.JobNeighborhoodsFactory;
import com.graphhopper.jsprit.core.algorithm.ruin.distance.AvgServiceAndShipmentDistance;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GreedyByNeighborsInsertion extends GreedyInsertion {
    final double distanceDiffForSameLocation;
    private static Logger logger = LoggerFactory.getLogger(GreedyByNeighborsInsertion.class);

    Map<String, Collection<Job>> jobsThaHavToBeInSameRoute = new HashMap<>();
    Map<String, Integer> jobsThaHavToBeInSameRouteSize = new HashMap<>();

    Comparator<Job> withMostNeighborsComparator = new Comparator<Job>() {
        @Override
        public int compare(Job job1, Job job2) {
            int numJobsNearToJob1 = jobsThaHavToBeInSameRouteSize.containsKey(job1.getId()) ? jobsThaHavToBeInSameRouteSize.get(job1.getId()) : 0;
            int numJobsNearToJob2 = jobsThaHavToBeInSameRouteSize.containsKey(job2.getId()) ? jobsThaHavToBeInSameRouteSize.get(job2.getId()) : 0;
            return numJobsNearToJob2 - numJobsNearToJob1;
        }
    };

    public GreedyByNeighborsInsertion(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem, double distanceDiffForSameLocationMeter) {
        super(jobInsertionCalculator, vehicleRoutingProblem);
        this.distanceDiffForSameLocation = distanceDiffForSameLocationMeter;
        initializeNeighbors();
    }


    @Override
    public String toString() {
        return "[name=greedyByNeighborhoodInsertion]";
    }

    Map<String, Integer> initializeNeighbors() {
        JobNeighborhoods neighborhoods = new JobNeighborhoodsFactory().createNeighborhoods(vrp, new AvgServiceAndShipmentDistance(vrp.getTransportCosts()));
        neighborhoods.initialise();
        for (Job job : vrp.getJobs().values()) {
            Location location = getLocation(job);
            Iterator<Job> nearestNeighborsIterator = neighborhoods.getNearestNeighborsIterator(vrp.getJobs().size(), job);
            HashSet<Job> nearestJobs = new HashSet<>();
            while (nearestNeighborsIterator.hasNext()) {
                Job next = nearestNeighborsIterator.next();
                if (distanceDiffForSameLocation >= vrp.getTransportCosts().getDistance(location, getLocation(next), 0, VehicleImpl.createNoVehicle()))
                    nearestJobs.add(next);
                else break;
            }
            jobsThaHavToBeInSameRoute.put(job.getId(), nearestJobs);
            jobsThaHavToBeInSameRouteSize.put(job.getId(), nearestJobs.size());
        }
        return jobsThaHavToBeInSameRouteSize;
    }

    @Override
    public Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        List<Job> jobsToInsert = new ArrayList<>(unassignedJobs);
        Set<Job> failedToAssign = new HashSet<>(insertBreaks(vehicleRoutes, jobsToInsert));

        Collections.sort(jobsToInsert, withMostNeighborsComparator);
        while (!jobsToInsert.isEmpty()) {
            Job withMostNeighbors = jobsToInsert.remove(0);
            failedToAssign.addAll(insertJobWithNearest(vehicleRoutes, withMostNeighbors, jobsToInsert));
        }
        return failedToAssign;
    }

    private Collection<Job> insertJobWithNearest(Collection<VehicleRoute> vehicleRoutes, Job withMostNeighbors, List<Job> jobsToInsert) {
        List<Job> jobs = new ArrayList<>();
        jobs.add(withMostNeighbors);
        Collection<Job> failedToInsert = super.insertUnassignedJobs(vehicleRoutes, jobs);
        if (!failedToInsert.isEmpty())
            return failedToInsert;

        VehicleRoute route = findRouteThatServesJob(vehicleRoutes, withMostNeighbors);
        if (route != null && jobsThaHavToBeInSameRoute.containsKey(withMostNeighbors.getId())) {
            Collection<Job> jobCollection = jobsThaHavToBeInSameRoute.get(withMostNeighbors.getId());
            for (Job job : jobCollection) {
                if (jobsToInsert.contains(job)) {
                    InsertionData iData = bestInsertionCalculator.getInsertionData(route, job, NO_NEW_VEHICLE_YET, NO_NEW_DEPARTURE_TIME_YET, NO_NEW_DRIVER_YET, Double.MAX_VALUE);
                    if (!(iData instanceof InsertionData.NoInsertionFound)) {
                        super.insertJob(job, iData, route);
                        jobsToInsert.remove(job);
                    }
                }
            }
        } else {
            logger.error("this should not happen route {} jobsThaHavToBeInSameRoute contains key {}", route, jobsThaHavToBeInSameRoute.containsKey(withMostNeighbors.getId()));
        }

        return failedToInsert;
    }
}