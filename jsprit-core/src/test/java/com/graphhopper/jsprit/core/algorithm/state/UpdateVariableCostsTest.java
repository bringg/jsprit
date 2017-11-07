/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.core.algorithm.state;

import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.util.ActivityTimeTracker;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class UpdateVariableCostsTest {

    VehicleRoutingActivityCosts activityCosts = Mockito.mock(VehicleRoutingActivityCosts.class);
    VehicleRoutingTransportCosts transportCosts = Mockito.mock(VehicleRoutingTransportCosts.class);
    StateManager stateManager = Mockito.mock(StateManager.class);
    ActivityTimeTracker timeTracker = Mockito.mock(ActivityTimeTracker.class);

    UpdateVariableCosts updateVariableCosts = new UpdateVariableCosts(
        activityCosts,
        transportCosts,
        stateManager,
        ActivityTimeTracker.ActivityPolicy.AS_SOON_AS_ARRIVED,
        timeTracker);

    @Test
    public void testWithTimeTrackerBegin() {
        VehicleRoute route = Mockito.mock(VehicleRoute.class);
        doNothing().when(timeTracker).begin(route);
        updateVariableCosts.begin(route);
        verify(timeTracker, times(1)).begin(route);
    }
}
