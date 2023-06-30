package com.spertus.jacquard.syntaxgrader;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Preconditions;
import com.spertus.jacquard.common.Result;

import java.util.*;

/**
 * Checks whether the correct modifiers are used for fields (instance and
 * static variables/constants). For example, this could be used to verify
 * that certain instance variables are declared {@code private} or {@code final}.
 */
public class FieldModifierChecker extends SyntaxChecker {
    private final List<String> varNames;

    /**
     * Creates a field modifier checker, where a field declaration passes if
     * it contains all the required modifiers and no modifiers that are neither
     * required nor optional. If there are 10 fields, each with a maximum score
     * of 1.0, the maximum score produced by this checker will be 10.0.
     *
     * @param name                the name of this checker
     * @param maxScorePerInstance the per field score if the check succeeds
     * @param fieldNames          the names of the fields to check
     * @param requiredModifiers   modifiers that must be used
     * @param optionalModifiers   modifiers that may be used
     */
    public FieldModifierChecker(String name, double maxScorePerInstance, List<String> fieldNames, List<Modifier> requiredModifiers, List<Modifier> optionalModifiers) {
        super(name, maxScorePerInstance, null);
        this.varNames = fieldNames;
        adapter = new Adapter(requiredModifiers, optionalModifiers);
    }

    /**
     * Creates a field modifier checker.
     *
     * @param name              the name, which is used in the {@link Result}
     * @param maxScore          the maximum score for each variable
     * @param varNames          the names of the variables to check
     * @param requiredModifiers modifiers that should be used on each variable
     * @param optionalModifiers modifiers that may be used on variables
     * @return a new instance
     */
    public static FieldModifierChecker makeChecker(
            String name,
            double maxScore,
            List<String> varNames,
            List<Modifier> requiredModifiers,
            List<Modifier> optionalModifiers
    ) {
        return new FieldModifierChecker(
                name,
                maxScore,
                varNames,
                requiredModifiers,
                optionalModifiers
        );
    }

    /**
     * Creates a field modifier checker with a default name.
     *
     * @param maxScore          the maximum score for each variable
     * @param varNames          the names of the variables to check
     * @param requiredModifiers modifiers that should be used on each variable
     * @param optionalModifiers modifiers that may be used on variables
     * @return a new instance
     */
    public static FieldModifierChecker makeChecker(
            double maxScore,
            List<String> varNames,
            List<Modifier> requiredModifiers,
            List<Modifier> optionalModifiers
    ) {
        return makeChecker(
                null,
                maxScore,
                varNames, requiredModifiers, optionalModifiers
        );
    }

    @Override
    public double getTotalMaxScore() {
        return maxScorePerInstance * varNames.size();
    }

    private class Adapter extends VoidVisitorAdapter<List<Result>> {
        private final List<Modifier> requiredModifiers;
        private final List<Modifier> optionalModifiers;

        private Adapter(List<Modifier> requiredModifiers, List<Modifier> optionalModifiers) {
            super();
            this.requiredModifiers = new ArrayList<>(requiredModifiers);
            this.optionalModifiers = new ArrayList<>(optionalModifiers);
        }

        @Override
        public void visit(VariableDeclarator vd, List<Result> collector) {
            try {
                if (isField(vd) && varNames.contains(vd.getNameAsString())) {
                    final FieldDeclaration fd = getFieldDeclaration(vd);

                    // Make a copy of this instance variable's modifiers.
                    final List<Modifier> modifiers = new ArrayList<>(fd.getModifiers());

                    // Ensure that all required modifiers are present, removing them.
                    for (final Modifier modifier : requiredModifiers) {
                        if (modifiers.contains(modifier)) {
                            modifiers.remove(modifier);
                        } else {
                            collector.add(
                                    makeFailingResult(
                                            String.format(
                                                    "%s is missing required modifier '%s'.",
                                                    getDeclarationDescription(fd, vd),
                                                    modifier.toString().trim())));
                            return;
                        }
                    }

                    // Ensure that any remaining modifiers are permitted.
                    for (final Modifier modifier : modifiers) {
                        if (!optionalModifiers.contains(modifier)) {
                            collector.add(
                                    makeFailingResult(
                                            String.format(
                                                    "%s has forbidden modifier '%s'.",
                                                    getDeclarationDescription(fd, vd),
                                                    modifier.toString().trim())));
                            return;
                        }
                    }
                    collector.add(makeSuccessResult(
                            String.format(
                                    "%s.%s is declared correctly.",
                                    getEnclosingClassName(fd),
                                    getDeclarationDescription(fd, vd))));
                }
            } finally {
                super.visit(vd, collector);
            }
        }
    }

    private String getDeclarationDescription(FieldDeclaration fd, VariableDeclarator vd) {
        return String.format(
                "Declaration of %s variable %s in class %s",
                fd.getModifiers().contains(Modifier.staticModifier()) ? "static" : "instance",
                vd.getNameAsString(),
                getEnclosingClassName(fd));
    }

    private String getEnclosingClassName(FieldDeclaration fd) {
        if (fd.getParentNode().isPresent() &&
                fd.getParentNode().get() instanceof ClassOrInterfaceDeclaration classOrInterface) {
            return classOrInterface.getNameAsString();
        } else {
            return "CLASS UNKNOWN";
        }
    }

    private boolean isField(VariableDeclarator vd) {
        return vd.getParentNode().isPresent()
                && vd.getParentNode().get() instanceof FieldDeclaration;
    }

    // This should be called only if isField() is true.
    private FieldDeclaration getFieldDeclaration(VariableDeclarator vd) {
        Preconditions.checkState(isField(vd));
        // The precondition guarantees the safety of the get() and cast.
        return (FieldDeclaration) vd.getParentNode().get();
    }
}
