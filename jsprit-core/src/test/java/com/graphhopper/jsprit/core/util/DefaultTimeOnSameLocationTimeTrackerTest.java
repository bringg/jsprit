package com.graphhopper.jsprit.core.util;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.ForwardTransportTime;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.Start;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Random;

public class DefaultTimeOnSameLocationTimeTrackerTest {
    @Test
    public void visit_sameLocation() throws Exception {
        ForwardTransportTime transportTime = Mockito.mock(ForwardTransportTime.class);
        VehicleRoutingActivityCosts activityCosts = Mockito.mock(VehicleRoutingActivityCosts.class);
        Random random = new Random();
        double activityDurationOnSameLocation = random.nextInt(10) + random.nextDouble();
        DefaultTimeOnSameLocationTimeTracker defaultTimeOnSameLocationTimeTracker = new DefaultTimeOnSameLocationTimeTracker(transportTime, ActivityTimeTracker.ActivityPolicy.AS_SOON_AS_ARRIVED, activityCosts, activityDurationOnSameLocation);
        Location prevAct = Location.newInstance(random.nextInt(100) + random.nextDouble(), random.nextInt(100) + random.nextDouble());
        VehicleRoute route = Mockito.mock(VehicleRoute.class);

        Mockito.when(route.getVehicle()).thenReturn(Mockito.mock(Vehicle.class));
        Mockito.when(route.getDriver()).thenReturn(Mockito.mock(Driver.class));


        double startTime = random.nextInt(10) + random.nextDouble();
        Start start = new Start(prevAct, startTime, startTime + 5 + random.nextDouble());
        Mockito.when(route.getStart()).thenReturn(start);
        defaultTimeOnSameLocationTimeTracker.begin(route);

        TourActivity tourActivity = Mockito.mock(TourActivity.class);
        Mockito.when(activityCosts.getActivityDuration(
            tourActivity,
            defaultTimeOnSameLocationTimeTracker.actArrTime,
            defaultTimeOnSameLocationTimeTracker.route.getDriver(),
            defaultTimeOnSameLocationTimeTracker.route.getVehicle())
        ).thenReturn(5.0);

        Mockito.when(transportTime.getTransportTime(
            prevAct,
            prevAct,
            defaultTimeOnSameLocationTimeTracker.actArrTime,
            defaultTimeOnSameLocationTimeTracker.route.getDriver(),
            defaultTimeOnSameLocationTimeTracker.route.getVehicle())
        ).thenReturn(0.0);

        Mockito.when(tourActivity.getLocation()).thenReturn(prevAct);

        defaultTimeOnSameLocationTimeTracker.visit(tourActivity);

        assert(defaultTimeOnSameLocationTimeTracker.actEndTime == defaultTimeOnSameLocationTimeTracker.actArrTime + activityDurationOnSameLocation - 5);
    }

    @Test
    public void visit_notTheSameLocation() throws Exception {
        ForwardTransportTime transportTime = Mockito.mock(ForwardTransportTime.class);
        VehicleRoutingActivityCosts activityCosts = Mockito.mock(VehicleRoutingActivityCosts.class);
        Random random = new Random();
        double activityDurationOnSameLocation = random.nextInt(10) + random.nextDouble();
        DefaultTimeOnSameLocationTimeTracker defaultTimeOnSameLocationTimeTracker = new DefaultTimeOnSameLocationTimeTracker(transportTime, ActivityTimeTracker.ActivityPolicy.AS_SOON_AS_ARRIVED, activityCosts, activityDurationOnSameLocation);
        Location prevAct = Location.newInstance(random.nextInt(100) + random.nextDouble(), random.nextInt(100) + random.nextDouble());
        VehicleRoute route = Mockito.mock(VehicleRoute.class);

        Mockito.when(route.getVehicle()).thenReturn(Mockito.mock(Vehicle.class));
        Mockito.when(route.getDriver()).thenReturn(Mockito.mock(Driver.class));


        double startTime = random.nextInt(10) + random.nextDouble();
        Start start = new Start(prevAct, startTime, startTime + 5 + random.nextDouble());
        Mockito.when(route.getStart()).thenReturn(start);
        defaultTimeOnSameLocationTimeTracker.begin(route);

        Location currentAct = Location.newInstance(random.nextInt(100) + random.nextDouble(), random.nextInt(100) + random.nextDouble());

        defaultTimeOnSameLocationTimeTracker.begin(route);

        TourActivity tourActivity = Mockito.mock(TourActivity.class);
        Mockito.when(activityCosts.getActivityDuration(
            tourActivity,
            defaultTimeOnSameLocationTimeTracker.startAtPrevAct,
            defaultTimeOnSameLocationTimeTracker.route.getDriver(),
            defaultTimeOnSameLocationTimeTracker.route.getVehicle())
        ).thenReturn(5.0);

        Mockito.when(transportTime.getTransportTime(
            prevAct,
            currentAct,
            defaultTimeOnSameLocationTimeTracker.actArrTime,
            defaultTimeOnSameLocationTimeTracker.route.getDriver(),
            defaultTimeOnSameLocationTimeTracker.route.getVehicle())
        ).thenReturn(0.0);

        Mockito.when(tourActivity.getLocation()).thenReturn(currentAct);

        defaultTimeOnSameLocationTimeTracker.visit(tourActivity);

        assert(defaultTimeOnSameLocationTimeTracker.actEndTime == defaultTimeOnSameLocationTimeTracker.actArrTime + 5);
    }

}
