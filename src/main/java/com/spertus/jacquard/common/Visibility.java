package com.spertus.jacquard.common;

/**
 * The visibility of a {@link Result}.
 * This reuses code from <a href="https://github.com/tkutcher/jgrade">jgrade</a>
 * by Tim Kutcher.
 */
public enum Visibility {
    /**
     * Visible to the student immediately.
     */
    VISIBLE("visible"),

    /**
     * Never visible to the student.
     */
    HIDDEN("hidden"),

    /**
     * Visible to the student after the due date.
     */
    AFTER_DUE_DATE("after_due_date"),

    /**
     * Visible to the student when grading is complete.
     */
    AFTER_PUBLISHED("after_published");

    private final String gradescopeText;

    Visibility(final String gradescopeText) {
        this.gradescopeText = gradescopeText;
    }

    /**
     * Gets a textual representation suitable for Gradescope.
     *
     * @return a textual representation suitable for Gradescope
     */
    public String getGradescopeText() {
        return gradescopeText;
    }
}
