package com.spertus.jacquard.coverage;

/**
 * A linear scorer that considers only line coverage, not branch coverage.
 * For example, if {@link #score(double, double)} is called on a linear scorer
 * with a maximum score of 10 and line coverage of .95, the score 9.5 would be
 * returned, regardless of the branch coverage percent.
 */
public class LinearLineScorer extends LinearScorer {
    /**
     * Creates a linear scorer that considers only line coverage, not
     * branch coverage.
     *
     * @param maxPoints the maximum number of points
     */
    public LinearLineScorer(final double maxPoints) {
       super(0, maxPoints);
    }

    @Override
    protected String getMessage(final double branchCoverage, final double lineCoverage) {
        return String.format("Line coverage is %.0f%%", 100 * lineCoverage);
    }
}
