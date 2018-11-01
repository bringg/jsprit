package com.graphhopper.jsprit.core.problem.job;

public class BreakForMultipleTimeWindows extends Break {

    BreakForMultipleTimeWindows(Break.Builder builder) {
        super(builder);
    }

    public static class Builder extends Break.Builder {
        Builder(String id) {
            super(id);
        }

        public BreakForMultipleTimeWindows build() {
            super.build();
            return new BreakForMultipleTimeWindows(this);
        }
    }

}
