package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.DriverImpl;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleFleetManager;
import com.graphhopper.jsprit.core.util.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GreedyInsertionByAverage extends GreedyInsertion {
    public static final Coordinate NO_COORDINATE = new Coordinate(0, 0);
    final double ratioToSelectNearest;
    final double ratioToSelectRandom;
    final double ratioToSelectFarthest;
    final int nJobsToSelectFrom;
    private static Logger logger = LoggerFactory.getLogger(GreedyByNeighborsInsertion.class);
    private final VehicleRoutingTransportCosts transportCosts;
    private final VehicleFleetManager fleetManager;
    Map<String, Collection<Job>> jobsThaHaveToBeInSameRoute = new HashMap<>();

    public GreedyInsertionByAverage(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vrp, VehicleFleetManager fleetManager) {
        this(jobInsertionCalculator, vrp, fleetManager, 0.33, 0.33, 0.33, 3);
    }

    public GreedyInsertionByAverage(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vrp, VehicleFleetManager fleetManager,
                                    double ratioToSelectNearest, double ratioToSelectRandom, double ratioToSelectFarthest, int nJobsToSelectFrom) {
        super(jobInsertionCalculator, vrp);
        this.fleetManager = fleetManager;
        double sum = ratioToSelectFarthest + ratioToSelectRandom + ratioToSelectRandom;
        this.ratioToSelectRandom = ratioToSelectRandom / sum;
        this.ratioToSelectNearest = this.ratioToSelectRandom + ratioToSelectNearest / sum;
        this.ratioToSelectFarthest = this.ratioToSelectNearest + ratioToSelectFarthest / sum;
        this.nJobsToSelectFrom = nJobsToSelectFrom;
        transportCosts = vrp.getTransportCosts();
    }


    @Override
    public String toString() {
        return "[name=greedyByNeighborhoodInsertion]";
    }

    @Override
    public Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        final List<Job> jobsToInsert = new ArrayList<>(unassignedJobs);
        Set<Job> failedToAssign = new HashSet<>(insertBreaks(vehicleRoutes, jobsToInsert));
        ArrayList<VehicleRoute> openRoutes = new ArrayList<>(vehicleRoutes);
        while(!openRoutes.isEmpty()) {
            VehicleRoute route = openRoutes.remove(0);
            while (!jobsToInsert.isEmpty()) {
                RouteAndJob nextJobToInsert = getNextJobToInsert(jobsToInsert, route);
                if (nextJobToInsert == null)
                    return getAllUnassigned(jobsToInsert, failedToAssign);

                insertJobsToRoute(vehicleRoutes, jobsToInsert, nextJobToInsert);
            }
        }

        if (openRoutes.isEmpty()) {
            while (!jobsToInsert.isEmpty()) {
                RouteAndJob insertionData = getNextJobToInsert(jobsToInsert, null);
                if (insertionData == null)
                    return getAllUnassigned(jobsToInsert, failedToAssign);

                insertJobsToRoute(vehicleRoutes, jobsToInsert, insertionData);
            }
        }
        return getAllUnassigned(jobsToInsert, failedToAssign);
    }

    private Collection<Job> getAllUnassigned(List<Job> jobsToInsert, Set<Job> failedToAssign) {
        failedToAssign.addAll(jobsToInsert);
        return failedToAssign;
    }

    private void insertJobsToRoute(Collection<VehicleRoute> vehicleRoutes, List<Job> jobsToInsert, RouteAndJob insertionData) {
        super.insertJob(insertionData.job, insertionData.insertionData, insertionData.vehicleRoute);
        jobsToInsert.remove(insertionData.job);
        vehicleRoutes.add(insertionData.vehicleRoute);
        while (!jobsToInsert.isEmpty()) {
            RouteAndJob nextJobToInsert = getNextJobToInsert(jobsToInsert, insertionData.vehicleRoute);
            if (nextJobToInsert != null) {
                super.insertJob(nextJobToInsert.job, nextJobToInsert.insertionData, nextJobToInsert.vehicleRoute);
                jobsToInsert.remove(nextJobToInsert.job);
            }
        }
    }

    private RouteAndJob getNextJobToInsert(List<Job> jobsToInsert, VehicleRoute vehicleRoute) {
        if (vehicleRoute == null || vehicleRoute.isEmpty()) {
            return selectRouteAndJob(jobsToInsert);
        }

        Coordinate center = getCenter(vehicleRoute.getTourActivities().getJobs());
        if (center == null)
            return selectRouteAndJob(jobsToInsert);

        List<Job> sortedJobs = getNearestJobs(jobsToInsert, vehicleRoute.getVehicle(), Location.newInstance(center.getX(), center.getY()), jobsToInsert.size());
        ArrayList<VehicleRoute> vehicleRoutes = new ArrayList<>();
        vehicleRoutes.add(vehicleRoute);
        while (!sortedJobs.isEmpty()) {
            List<Job> nextJobs = new ArrayList<>(sortedJobs.subList(0, Math.min(nJobsToSelectFrom, sortedJobs.size())));
            ScoredJob scoredJob = nextJob(vehicleRoutes, nextJobs, new ArrayList<ScoredJob>());
            if (scoredJob != null && !(scoredJob.getInsertionData() instanceof InsertionData.NoInsertionFound))
                return new RouteAndJob(scoredJob.getJob(), scoredJob.getRoute(), scoredJob.getInsertionData());
            sortedJobs.removeAll(nextJobs);
        }
        return null;
    }

    private RouteAndJob selectRouteAndJob(List<Job> jobsToInsert) {
        ArrayList<Vehicle> vehicles = new ArrayList<>(fleetManager.getAvailableVehicles());
        Collections.shuffle(vehicles);

        double nextDouble = random.nextDouble();
        while (!vehicles.isEmpty()) {
            Vehicle vehicle = vehicles.remove(0);
            VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle).build();
            ArrayList<Job> jobs = new ArrayList<>(jobsToInsert);
            while (!jobs.isEmpty()) {
                Job job;
                if (nextDouble < ratioToSelectNearest) {
                    job = getNearestJob(jobs, vehicle);
                } else if (nextDouble < ratioToSelectFarthest) {
                    job = getFarthestJob(jobs, vehicle);
                } else {
                    job = getRandomJob(jobs);
                }
                InsertionData insertionData = bestInsertionCalculator.getInsertionData(route, job, vehicle, vehicle.getEarliestDeparture(), DriverImpl.noDriver(), Double.MAX_VALUE);
                if (!(insertionData instanceof InsertionData.NoInsertionFound))
                    return new RouteAndJob(job, route, insertionData);
                jobs.remove(job);
            }
        }
        return null;
    }

    private static Coordinate getCenter(Collection<Job> jobs) {
        double sumLat = 0, sumLng = 0, sumCoordinates = 0;
        for (Job job : jobs) {
            Location jobLocation = getJobLocation(job);
            if (jobLocation != null && !jobLocation.getCoordinate().equals(NO_COORDINATE)) {
                sumCoordinates++;
                sumLat += jobLocation.getCoordinate().getY();
                sumLng += jobLocation.getCoordinate().getX();
            }
        }
        if (sumCoordinates == 0)
            return null;
        return new Coordinate(sumLng / sumCoordinates, sumLat / sumCoordinates);
    }

    private Job getRandomJob(List<Job> jobsToInsert) {
        return jobsToInsert.get(random.nextInt(jobsToInsert.size()));
    }

    private Job getFarthestJob(List<Job> jobsToInsert, Vehicle vehicle) {
        double maxTravelTime = -1;
        Job farthestJob = null;
        for (Job job : jobsToInsert) {
            double transportTime = getTransportTime(vehicle, job, -1);
            if (transportTime > maxTravelTime) {
                maxTravelTime = transportTime;
                farthestJob = job;
            }
        }
        return farthestJob;
    }

    private double getTransportTime(Vehicle vehicle, Job job, double transportTime) {
        Location jobLocation = getJobLocation(job);
        if (jobLocation == null) {
            return transportTime;
        }
        return transportCosts.getTransportTime(vehicle.getStartLocation(), jobLocation, vehicle.getEarliestDeparture(), DriverImpl.noDriver(), vehicle);
    }

    static Location getJobLocation(Job job) {
        if (job instanceof Service)
            return ((Service) job).getLocation();
        if (job instanceof Shipment)
            return ((Shipment) job).getDeliveryLocation();
        return null;
    }

    private Job getNearestJob(List<Job> jobsToInsert, Vehicle vehicle) {
        return getNearestJobs(jobsToInsert, vehicle, vehicle.getStartLocation(), 1).get(0);
    }

    private List<Job> getNearestJobs(final List<Job> jobsToInsert, final Vehicle vehicle, final Location location, int n) {
        ArrayList<Job> sortedJobs = new ArrayList<>(jobsToInsert);
        if (jobsToInsert.size() < n)
            return sortedJobs;

        final Map<String, Double> transportTimes = new HashMap<>();
        for (Job job : jobsToInsert) {
            transportTimes.put(job.getId(), transportCosts.getTransportTime(location, getJobLocation(job), vehicle.getEarliestDeparture(), DriverImpl.noDriver(), vehicle));
        }

        Collections.sort(sortedJobs, new Comparator<Job>() {
            @Override
            public int compare(Job job1, Job job2) {
                return (int) (transportTimes.get(job1.getId()) - transportTimes.get(job2.getId()));
            }
        });
        return new ArrayList<>(sortedJobs.subList(0, n));

    }

    private final class RouteAndJob {
        private final Job job;
        private final VehicleRoute vehicleRoute;
        private final InsertionData insertionData;

        private RouteAndJob(Job job, VehicleRoute vehicleRoute, InsertionData insertionData) {
            this.job = job;
            this.vehicleRoute = vehicleRoute;
            this.insertionData = insertionData;
        }
    }
}
