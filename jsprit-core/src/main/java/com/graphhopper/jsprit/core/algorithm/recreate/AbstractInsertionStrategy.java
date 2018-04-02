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

package com.graphhopper.jsprit.core.algorithm.recreate;


import com.graphhopper.jsprit.core.algorithm.recreate.listener.InsertionListener;
import com.graphhopper.jsprit.core.algorithm.recreate.listener.InsertionListeners;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.RandomNumberGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractInsertionStrategy implements InsertionStrategy {

    protected class Insertion {

        private final VehicleRoute route;

        private final InsertionData insertionData;

        public Insertion(VehicleRoute vehicleRoute, InsertionData insertionData) {
            super();
            this.route = vehicleRoute;
            this.insertionData = insertionData;
        }

        public VehicleRoute getRoute() {
            return route;
        }

        public InsertionData getInsertionData() {
            return insertionData;
        }

    }

    private final static Logger logger = LoggerFactory.getLogger(AbstractInsertionStrategy.class);

    protected Random random = RandomNumberGeneration.getRandom();

    protected final static double NO_NEW_DEPARTURE_TIME_YET = -12345.12345;

    protected final static Vehicle NO_NEW_VEHICLE_YET = null;

    protected final static Driver NO_NEW_DRIVER_YET = null;

    private InsertionListeners insertionsListeners;

    private EventListeners eventListeners;

    protected VehicleRoutingProblem vrp;

    protected Map<String, Integer> driversCountBySkills = new HashMap<>();

    public AbstractInsertionStrategy(VehicleRoutingProblem vrp) {
        this.insertionsListeners = new InsertionListeners();
        this.vrp = vrp;
        eventListeners = new EventListeners();
        initializeDriversCountBySkills();
    }

    private void initializeDriversCountBySkills() {
        for (Vehicle vehicle : vrp.getVehicles()) {
            for (String skill : vehicle.getSkills().values()) {
                Integer count = driversCountBySkills.get(skill);
                if (count == null)
                    count = 0;
                driversCountBySkills.put(skill, count + 1);
            }
        }
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public Collection<Job> insertJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        insertionsListeners.informInsertionStarts(vehicleRoutes, unassignedJobs);
        Collection<Job> badJobs = insertUnassignedJobs(vehicleRoutes, unassignedJobs);
        insertionsListeners.informInsertionEndsListeners(vehicleRoutes);
        return badJobs;
    }

    public void markUnassigned(Job unassigned, List<String> reasons) {
        insertionsListeners.informJobUnassignedListeners(unassigned, reasons);
    }

    public abstract Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs);

    @Override
    public void removeListener(InsertionListener insertionListener) {
        insertionsListeners.removeListener(insertionListener);
    }

    @Override
    public Collection<InsertionListener> getListeners() {
        return Collections.unmodifiableCollection(insertionsListeners.getListeners());
    }

    @Override
    public void addListener(InsertionListener insertionListener) {
        insertionsListeners.addListener(insertionListener);

    }

    protected void insertJob(Job unassignedJob, InsertionData iData, VehicleRoute inRoute) {
        logger.trace("insert: [jobId={}]{}", unassignedJob.getId(), iData);
        insertionsListeners.informBeforeJobInsertion(unassignedJob, iData, inRoute);
        if (!(inRoute.getVehicle().getId().equals(iData.getSelectedVehicle().getId()))) {
            insertionsListeners.informVehicleSwitched(inRoute, inRoute.getVehicle(), iData.getSelectedVehicle());
        }
        for (Event e : iData.getEvents()) {
            eventListeners.inform(e);
        }
        insertionsListeners.informJobInserted(unassignedJob, inRoute, iData.getInsertionCost(), iData.getAdditionalTime());
    }

    protected static void updateNewRouteInsertionData(InsertionData iData) {
        if (iData.getSelectedVehicle() != null)
            iData.setInsertionCost(iData.getInsertionCost() + iData.getSelectedVehicle().getType().getVehicleCostParams().fix);
    }


    protected void insertBreak(JobInsertionCostsCalculator jobInsertionCostsCalculator, List<Job> badJobs, VehicleRoute route, InsertionData iDataJob) {
        if (iDataJob.getSelectedVehicle() != null && iDataJob.getSelectedVehicle().getBreak() != null) {
            final Break aBreak = iDataJob.getSelectedVehicle().getBreak();
            InsertionData iData = jobInsertionCostsCalculator.getInsertionData(route, aBreak, iDataJob.getSelectedVehicle(), iDataJob.getSelectedVehicle().getEarliestDeparture(), iDataJob.getSelectedDriver(), Double.MAX_VALUE);
            if (iData instanceof InsertionData.NoInsertionFound) {
                badJobs.add(aBreak);
            } else {
                insertJob(aBreak, iData, route);
            }
        }
    }

}
