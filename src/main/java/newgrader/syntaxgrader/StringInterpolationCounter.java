package newgrader.syntaxgrader;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import newgrader.Result;

/**
 * Counter of number of occurrences of string interpolation.
 */
public class StringInterpolationCounter extends Counter {
    /**
     * Create a new string interpolation counter.
     *
     * @param name     the name of this processor (for the {@link Result})
     * @param maxScore the score if the condition holds
     * @param minCount the minimum number of occurrences
     * @param maxCount the maximum number of occurrences, or {@link Integer#MAX_VALUE}
     *                 if there is no limit
     * @throws IllegalArgumentException if minCount &lt; 0, maxCount &lt; minCount,
     *                                  or minCount is 0 when maxCount is {@link Integer#MAX_VALUE}
     */
    public StringInterpolationCounter(String name, int maxScore, int minCount, int maxCount) {
        super(name, "string interpolations", maxScore, minCount, maxCount, new StringInterpolationAdapter());
    }

    private static class StringInterpolationAdapter extends VoidVisitorAdapter<MutableInteger> {
        @Override
        public void visit(MethodCallExpr node, MutableInteger mi) {
            if (node.getScope().isPresent()) {
                String fullMethodName = node.getScope().get() + "." + node.getNameAsString();
                if ((fullMethodName.equals("System.out.printf") || fullMethodName.equals("String.format"))
                        && node.getArguments().size() > 1) {
                    mi.increment();
                }
            }
        }
    }
}