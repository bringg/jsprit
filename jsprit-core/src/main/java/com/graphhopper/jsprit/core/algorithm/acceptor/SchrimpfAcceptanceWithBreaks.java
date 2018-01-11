package com.graphhopper.jsprit.core.algorithm.acceptor;

import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.BreakActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import java.util.Collection;

public class SchrimpfAcceptanceWithBreaks extends SchrimpfAcceptance {
    private final SolutionCostCalculator solutionCostCalculator;
    public SchrimpfAcceptanceWithBreaks(SolutionCostCalculator solutionCostCalculator, int solutionMemory, double alpha) {
        super(solutionMemory, alpha);
        this.solutionCostCalculator = solutionCostCalculator;
    }

    @Override
    public boolean acceptSolution(Collection<VehicleRoutingProblemSolution> solutions, VehicleRoutingProblemSolution newSolution) {
        for (VehicleRoute route : newSolution.getRoutes()) {
            if (route.getVehicle().getBreak() != null) {
                boolean breakAdded = false;
                for (TourActivity activity : route.getTourActivities().getActivities()) {
                    breakAdded |= activity instanceof BreakActivity;
                }

                if (!breakAdded) {
                    newSolution.getRoutes().remove(route);
                    newSolution.getUnassignedJobs().addAll(route.getTourActivities().getJobs());
                }
            }
        }
        
        newSolution.setCost(solutionCostCalculator.getCosts(newSolution));

        return super.acceptSolution(solutions, newSolution);
    }

    @Override
    public String toString() {
        return "[name=SchrimpfAcceptanceWithBreaks]" + super.toString();
    }
}
