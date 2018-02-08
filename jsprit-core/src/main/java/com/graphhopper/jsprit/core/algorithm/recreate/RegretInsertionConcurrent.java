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

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Insertion based on regret approach.
 * <p>
 * <p>Basically calculates the insertion cost of the firstBest and the secondBest alternative. The score is then calculated as difference
 * between secondBest and firstBest, plus additional scoring variables that can defined in this.ScoringFunction.
 * The idea is that if the cost of the secondBest alternative is way higher than the first best, it seems to be important to insert this
 * customer immediatedly. If difference is not that high, it might not impact solution if this customer is inserted later.
 *
 * @author stefan schroeder
 */
public class RegretInsertionConcurrent extends RegretInsertion {


    private static Logger logger = LoggerFactory.getLogger(RegretInsertionConcurrentFast.class);

    private final ExecutorCompletionService<ScoredJob> completionService;

    public RegretInsertionConcurrent(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem, ExecutorService executorService) {
        super(jobInsertionCalculator, vehicleRoutingProblem);
        this.vrp = vehicleRoutingProblem;
        completionService = new ExecutorCompletionService<>(executorService);
        logger.debug("initialise " + this);
    }

    @Override
    protected ScoredJob nextJob(final Collection<VehicleRoute> routes, Collection<Job> unassignedJobList, List<ScoredJob> badJobList) {
        ScoredJob bestScoredJob = null;

        for (final Job unassignedJob : unassignedJobList) {
            completionService.submit(new Callable<ScoredJob>() {
                @Override
                public ScoredJob call() throws Exception {
                    return RegretInsertion.getScoredJob(routes, unassignedJob, insertionCostsCalculator, scoringFunction);
                }
            });
        }

        try {
            for (int i = 0; i < unassignedJobList.size(); i++) {
                Future<ScoredJob> fsj = completionService.take();
                ScoredJob sJob = fsj.get();
                if (sJob instanceof ScoredJob.BadJob) {
                    badJobList.add(sJob);
                    continue;
                }
                if (bestScoredJob == null) {
                    bestScoredJob = sJob;
                } else if (sJob.getScore() > bestScoredJob.getScore()) {
                    bestScoredJob = sJob;
                } else if (sJob.getScore() == bestScoredJob.getScore()) {
                    if (sJob.getJob().getId().compareTo(bestScoredJob.getJob().getId()) <= 0) {
                        bestScoredJob = sJob;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        return bestScoredJob;
    }
}
