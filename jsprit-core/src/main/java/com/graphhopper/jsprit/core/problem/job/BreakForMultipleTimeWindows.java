package com.graphhopper.jsprit.core.problem.job;

public class BreakForMultipleTimeWindows extends Break {

    public static class Builder extends Break.Builder {

        /**
         * Returns a new instance of builder that builds a pickup.
         *
         * @param id the id of the pickup
         * @return the builder
         */
        public static BreakForMultipleTimeWindows.Builder newInstance(String id) {
            return new BreakForMultipleTimeWindows.Builder(id);
        }

        Builder(String id) {
            super(id);
        }

        /**
         * Builds Pickup.
         * <p>
         * <p>Pickup type is "pickup"
         *
         * @return pickup
         * @throws IllegalStateException if neither locationId nor coordinate has been set
         */
        public BreakForMultipleTimeWindows build() {
            super.build();
            return new BreakForMultipleTimeWindows(this);
        }

    }

    BreakForMultipleTimeWindows(BreakForMultipleTimeWindows.Builder builder) {
        super(builder);
    }
}
