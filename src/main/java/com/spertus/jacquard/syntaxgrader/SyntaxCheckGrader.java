package com.spertus.jacquard.syntaxgrader;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.spertus.jacquard.common.Result;

import java.util.*;

/**
 * The base class for syntax-based graders that may involve multiple items of
 * the same type. Examples include:
 * <ul>
 *     <li> {@link MethodModifierGrader}, which checks whether
 *      each of a list of methods has the correct modifiers,
 *      awarding up to {@link #maxScorePerInstance} per method</li>
 *     <li> {@link ImportRequiredGrader}, which checks whether
 *     all required imports are present, awarding up to
 *     {@link #maxScorePerInstance} per import</li>
 *     <li> {@link ImportDisallowedGrader}, which checks whether any
 *     disallowed imports are present, awarding or withholding
 *     {@link #maxScorePerInstance} (all or nothing)</li>
 * </ul>
 */
public abstract class SyntaxCheckGrader extends SyntaxGrader {
    /**
     * The maximum score per instance checked.
     */
    protected final double maxScorePerInstance;

    /**
     * The adapter that visits the nodes of the parse tree. If this is not
     * passed to this class's constructor, it must be set in the child class's
     * constructor.
     */
    protected VoidVisitorAdapter<List<Result>> adapter;

    /**
     * Constructs a syntax checker. If the adapter is null, the constructor in
     * the concrete subclass must set it before returning. (Non-static adapters
     * that are inner classes cannot be created until this constructor has
     * completed.)
     *
     * @param name                the name of the syntax checker
     * @param maxScorePerInstance the maximum score per application of the check
     * @param adapter             the adapter or {@code null}
     */
    protected SyntaxCheckGrader(
            final String name,
            final double maxScorePerInstance,
            final VoidVisitorAdapter<List<Result>> adapter) {
        super(name);
        this.maxScorePerInstance = maxScorePerInstance;
        this.adapter = adapter;
    }

    /**
     * Performs any setup before a call to {@link #grade(CompilationUnit)}.
     */
    public void initialize() {
    }

    /**
     * Adds any results that cannot be computed until all visits are complete.
     *
     * @param results the list of results, which may be mutated by this call
     */
    public void finalizeResults(final List<Result> results) {
    }

    @Override
    public List<Result> grade(final CompilationUnit cu) {
        initialize();
        final List<Result> results = new ArrayList<>();
        adapter.visit(cu, results);
        finalizeResults(results);
        return results;
    }

    /**
     * Creates a result indicating total failure.
     *
     * @param message an accompanying message
     * @return the result
     */
    protected Result makeFailingResult(final String message) {
        return makeFailureResult(maxScorePerInstance, message);
    }

    /**
     * Creates a result indicating total success.
     *
     * @param message an accompanying message
     * @return the result
     */
    protected Result makeSuccessResult(final String message) {
        return makeSuccessResult(maxScorePerInstance, message);
    }
}
