package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;

import java.util.Collection;

public class GreedyInsertion extends RegretInsertion {

    public GreedyInsertion(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem) {
        super(jobInsertionCalculator, vehicleRoutingProblem);
    }


    protected static VehicleRoute findRoute(Collection<VehicleRoute> routes, Job job) {
        for (VehicleRoute r : routes) {
            if (r.getTourActivities().servesJob(job))
                return r;
        }
        return null;
    }

    protected final static Location getLocation(Job job) {
        if (job instanceof Service)
            return ((Service) job).getLocation();
        if (job instanceof Shipment)
            return ((Shipment) job).getDeliveryLocation();
        return null;
    }

}
