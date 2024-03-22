package com.spertus.jacquard.coverage;

import com.spertus.jacquard.common.*;
import com.spertus.jacquard.exceptions.*;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.*;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.*;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * A grader that uses Jacoco to measure code coverage of tests.
 */
public class CodeCoverageTester extends Tester {
    private static final String GRADER_NAME = "code coverage grader";
    private final String name;
    private final Scorer scorer;
    private final Class<?> classUnderTest;
    private final Class<?> testClass;

    /**
     * Creates a code coverage tester. The result depends on the {@code scorer}
     * and how much of the class under test is covered by the test class.
     *
     * @param name           the name
     * @param scorer         the scorer
     * @param classUnderTest the class under test
     * @param testClass      the test class
     */
    public CodeCoverageTester(
            final String name,
            final Scorer scorer,
            final Class<?> classUnderTest,
            final Class<?> testClass) {
        super();
        this.name = name;
        this.scorer = scorer;
        this.classUnderTest = classUnderTest;
        this.testClass = testClass;
    }

    /**
     * Creates a code coverage tester with a default name. The result depends on
     * the {@code scorer} and how much of the class under test is covered by the
     * test class.
     *
     * @param scorer         a scorer, which converts the outcome to a point value
     * @param classUnderTest the class under test
     * @param testClass      the test class
     */
    public CodeCoverageTester(
            final Scorer scorer,
            final Class<?> classUnderTest,
            final Class<?> testClass) {
        this(GRADER_NAME, scorer, classUnderTest, testClass);
    }

    private void instrument(
            final Instrumenter instrumenter,
            final MemoryClassLoader memoryClassLoader,
            final Class<?> clazz)
            throws IOException {
        try (InputStream is = readClassFile(clazz.getName())) {
            final byte[] instrumented = instrumenter.instrument(is, clazz.getName());
            memoryClassLoader.addDefinition(clazz.getName(), instrumented);
        }
    }

    private static InputStream readClassFile(final String name) {
        final String resource = '/' + name.replace('.', '/') + ".class";
        return CodeCoverageTester.class.getResourceAsStream(resource);
    }

    private void runJUnitTests(
            final MemoryClassLoader memoryClassLoader,
            final Class<?>... testClasses) {
        final List<? extends DiscoverySelector> selectors =
                Arrays.stream(testClasses)
                        .map(DiscoverySelectors::selectClass)
                        .collect(Collectors.toList());
        final CustomContextClassLoaderExecutor executor =
                new CustomContextClassLoaderExecutor(Optional.ofNullable(memoryClassLoader));
        executor.invoke(() -> executeTests(selectors));
    }

    private static int executeTests(final List<? extends DiscoverySelector> selectors) {
        final Launcher launcher = LauncherFactory.create();
        launcher.execute(request().selectors(selectors).build());
        return 0;
    }

    // This code is based on
    // https://www.jacoco.org/jacoco/trunk/doc/examples/java/CoreTutorial.java
    // by Marc R. Hoffmann and is
    // Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
    // and made available under
    // the terms of the Eclipse Public License 2.0 which is available at
    // http://www.eclipse.org/legal/epl-2.0
    private IClassCoverage calculateCoverage() throws Exception { // NOPMD
        final String cutName = classUnderTest.getName();
        final String testClassName = testClass.getName();
        final IRuntime runtime = new LoggerRuntime();
        final Instrumenter instrumenter = new Instrumenter(runtime);
        final MemoryClassLoader memoryClassLoader = new MemoryClassLoader();

        // Instrument the classes and add them to memoryClassLoader.
        instrument(instrumenter, memoryClassLoader, classUnderTest); // throws IOException
        instrument(instrumenter, memoryClassLoader, testClass); // throws IOException

        // Start data recording and run tests.
        final RuntimeData data = new RuntimeData(); // throws Exception
        runtime.startup(data); // throws Exception
        final Class<?> instrumentedTestClass = memoryClassLoader.loadClass(testClassName);
        runJUnitTests(memoryClassLoader, instrumentedTestClass);

        // Collect data.
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();

        // Calculate coverage.
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        try (InputStream is = readClassFile(cutName)) {
            analyzer.analyzeClass(is, cutName);
        }

        // Return coverage of the class under test.
        if (coverageBuilder.getClasses().size() == 1) {
            return coverageBuilder.getClasses().iterator().next();
        } else {
            throw new InternalException("Test coverage result retrieval failed.");
        }
    }

    @Override
    public List<Result> run() {
        try {
            final IClassCoverage cc = calculateCoverage();
            double branchCoverage = cc.getBranchCounter().getCoveredRatio();
            // Branch coverage could be NaN if the class under test had no code.
            if (!Double.isFinite(branchCoverage)) {
                branchCoverage = 1.0;
            }
            // Line coverage could be NaN if the class under test had no code.
            double lineCoverage = cc.getLineCounter().getCoveredRatio();
            if (!Double.isFinite(lineCoverage)) {
                lineCoverage = 1.0;
            }
            return List.of(
                    scorer.getResult(name, branchCoverage, lineCoverage));
        } catch (Exception e) { // NOPMD
            return List.of(Result.makeError("Unable to test code coverage (jacoco)", e));
        }
    }
}
