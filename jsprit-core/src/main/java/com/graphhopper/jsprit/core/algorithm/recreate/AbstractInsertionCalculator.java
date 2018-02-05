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

import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint.ConstraintsStatus;
import com.graphhopper.jsprit.core.problem.constraint.HardRouteConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by schroeder on 06/02/17.
 */
abstract class AbstractInsertionCalculator implements JobInsertionCostsCalculator {

    InsertionData checkRouteContraints(JobInsertionContext insertionContext, ConstraintManager constraintManager) {
        for (HardRouteConstraint hardRouteConstraint : constraintManager.getHardRouteConstraints()) {
            if (!hardRouteConstraint.fulfilled(insertionContext)) {
                InsertionData emptyInsertionData = new InsertionData.NoInsertionFound();
                emptyInsertionData.addFailedConstrainName(hardRouteConstraint.getClass().getSimpleName());
                return emptyInsertionData;
            }
        }
        return null;
    }

    ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime, Collection<String> failedActivityConstraints, ConstraintManager constraintManager) {
        ConstraintsStatus notFulfilled = null;
        List<String> failed = new ArrayList<>();
        FulfilledHelper fulfilledHelper = new FulfilledHelper(iFacts, prevAct, newAct, nextAct, prevActDepTime, failedActivityConstraints, constraintManager, notFulfilled, failed).invoke();
        if (fulfilledHelper.is())
            return fulfilledHelper.getStatus();
        notFulfilled = fulfilledHelper.getNotFulfilled();
//        ConstraintsStatus status = fulfilledHelper.getStatus();
        if (notFulfilled != null) {
            failedActivityConstraints.addAll(failed);
            return notFulfilled;
        }

        FulfilledHelper2 fulfilledHelper2 = new FulfilledHelper2(iFacts, prevAct, newAct, nextAct, prevActDepTime, failedActivityConstraints, constraintManager, notFulfilled, failed).invoke();
        if (fulfilledHelper2.is())
            return fulfilledHelper2.getStatus();
        notFulfilled = fulfilledHelper2.getNotFulfilled();
//        ConstraintsStatus status = fulfilledHelper2.getStatus();
        if (notFulfilled != null) {
            failedActivityConstraints.addAll(failed);
            return notFulfilled;
        }

        for (HardActivityConstraint constraint : constraintManager.getLowPrioHardActivityConstraints()) {
            ConstraintsStatus status = constraint.fulfilled(iFacts, prevAct, newAct, nextAct, prevActDepTime);
            if (status.equals(ConstraintsStatus.NOT_FULFILLED_BREAK) || status.equals(ConstraintsStatus.NOT_FULFILLED)) {
                failedActivityConstraints.add(constraint.getClass().getSimpleName());
                return status;
            }
        }
        return ConstraintsStatus.FULFILLED;
    }

    private class FulfilledHelper {
        private boolean myResult;
        private JobInsertionContext iFacts;
        private TourActivity prevAct;
        private TourActivity newAct;
        private TourActivity nextAct;
        private double prevActDepTime;
        private Collection<String> failedActivityConstraints;
        private ConstraintManager constraintManager;
        private ConstraintsStatus notFulfilled;
        private List<String> failed;
        private ConstraintsStatus status;

        public FulfilledHelper(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime, Collection<String> failedActivityConstraints, ConstraintManager constraintManager, ConstraintsStatus notFulfilled, List<String> failed) {
            this.iFacts = iFacts;
            this.prevAct = prevAct;
            this.newAct = newAct;
            this.nextAct = nextAct;
            this.prevActDepTime = prevActDepTime;
            this.failedActivityConstraints = failedActivityConstraints;
            this.constraintManager = constraintManager;
            this.notFulfilled = notFulfilled;
            this.failed = failed;
        }

        boolean is() {
            return myResult;
        }

        public ConstraintsStatus getNotFulfilled() {
            return notFulfilled;
        }

        public ConstraintsStatus getStatus() {
            return status;
        }

        public FulfilledHelper invoke() {
            for (HardActivityConstraint c : constraintManager.getCriticalHardActivityConstraints()) {
                status = c.fulfilled(iFacts, prevAct, newAct, nextAct, prevActDepTime);
                if (status.equals(ConstraintsStatus.NOT_FULFILLED_BREAK)) {
                    failedActivityConstraints.add(c.getClass().getSimpleName());
                    myResult = true;
                    return this;
                } else {
                    if (status.equals(ConstraintsStatus.NOT_FULFILLED)) {
                        failed.add(c.getClass().getSimpleName());
                        notFulfilled = status;
                    }
                }
            }
            myResult = false;
            return this;
        }
    }

    private class FulfilledHelper2 {
        private boolean myResult;
        private JobInsertionContext iFacts;
        private TourActivity prevAct;
        private TourActivity newAct;
        private TourActivity nextAct;
        private double prevActDepTime;
        private Collection<String> failedActivityConstraints;
        private ConstraintManager constraintManager;
        private ConstraintsStatus notFulfilled;
        private List<String> failed;
        private ConstraintsStatus status;

        public FulfilledHelper2(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime, Collection<String> failedActivityConstraints, ConstraintManager constraintManager, ConstraintsStatus notFulfilled, List<String> failed) {
            this.iFacts = iFacts;
            this.prevAct = prevAct;
            this.newAct = newAct;
            this.nextAct = nextAct;
            this.prevActDepTime = prevActDepTime;
            this.failedActivityConstraints = failedActivityConstraints;
            this.constraintManager = constraintManager;
            this.notFulfilled = notFulfilled;
            this.failed = failed;
        }

        boolean is() {
            return myResult;
        }

        public ConstraintsStatus getNotFulfilled() {
            return notFulfilled;
        }

        public ConstraintsStatus getStatus() {
            return status;
        }

        public FulfilledHelper2 invoke() {
            for (HardActivityConstraint c : constraintManager.getHighPrioHardActivityConstraints()) {
                status = c.fulfilled(iFacts, prevAct, newAct, nextAct, prevActDepTime);
                if (status.equals(ConstraintsStatus.NOT_FULFILLED_BREAK)) {
                    failedActivityConstraints.add(c.getClass().getSimpleName());
                    myResult = true;
                    return this;
                } else {
                    if (status.equals(ConstraintsStatus.NOT_FULFILLED)) {
                        failed.add(c.getClass().getSimpleName());
                        notFulfilled = status;
                    }
                }
            }
            myResult = false;
            return this;
        }
    }
}
