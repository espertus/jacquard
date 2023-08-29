package com.spertus.jacquard.common;

import com.spertus.jacquard.exceptions.ClientException;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The target of a {@link Grader}, such as a file or a directory.
 */
public final class Target {
    private static final String SUBMISSION_PATH_TEMPLATE =
            // arguments are package and class
            "src/main/java/%s/%s.java";

    // This works on Windows, even though it's not the normal separator.
    private static final char SEP = '/';

    private final Path path;

    private Target(final Path path) {
        this.path = path;
    }

    /**
     * Creates a target from a path, which could be to a file or directory.
     *
     * @param path the path
     * @return the target
     */
    public static Target fromPath(final Path path) {
        return new Target(path);
    }

    /**
     * Creates a target from a relative path string. It does not matter whether
     * forward slashes or backslashes are used as separators.
     *
     * @param s a relative path string
     * @return the target
     *
     * @deprecated Call {@link #fromClass(Class)} instead.
     */
    @Deprecated()
    public static Target fromPathString(final String s) {
        // https://stackoverflow.com/a/40163941/631051
        final Path absPath = FileSystems.getDefault().getPath(s).normalize().toAbsolutePath();
        return new Target(absPath);
    }

    /**
     * Creates a target from a class that the student is responsible for
     * submitting. The class's package must not be empty.
     *
     * @param targetClass the class
     * @return the target
     * @throws ClientException if the package of the class is invalid
     */
    public static Target fromClass(Class<?> targetClass) {
        String pkgName = targetClass.getPackageName();
        if (pkgName.isEmpty()) {
            throw new ClientException("Package name may not be empty.");
        }
        String pathString = String.format(SUBMISSION_PATH_TEMPLATE,
                pkgName.replace('.', SEP),
                targetClass.getSimpleName());
        return fromPathString(pathString);
    }

    /**
     * Creates targets from a directory string and list of files in the directory.
     *
     * @param dir   a relative directory string (ending with <code>/</code> or
     *              <code>\</code>)
     * @param files the names of the files in the directory
     * @return the target
     * @throws ClientException if dir does not end with a path separator
     */
    private static List<Target> fromPathStrings(final String dir, final String... files) {
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            throw new ClientException("dir must end with a path separator (/ or \\)");
        }
        return Arrays.stream(files)
                .map(file -> fromPathString(dir + file))
                .collect(Collectors.toList());
    }

    /**
     * Gets the string representation of this target's absolute path.
     *
     * @return the string representation of this target's absolute path
     */
    public String toPathString() {
        return path.toString();
    }

    /**
     * Gets this target's path
     *
     * @return this target's path
     */
    public Path toPath() {
        return path;
    }

    /**
     * Gets this target's file (which may be a directory).
     *
     * @return this target's file
     */
    public File toFile() {
        return new File(toPathString());
    }

    /**
     * Gets this target's directory. This is the target itself if it is a
     * directory; otherwise, it is the parent directory of the file.
     *
     * @return the directory
     */
    public Path toDirectory() {
        final File file = toFile();
        if (file.isDirectory()) {
            return file.toPath();
        } else {
            return file.getParentFile().toPath();
        }
    }

//    /**
//     * Parses this target, if it is a file.
//     *
//     * @param autograder autograder (for language level)
//     * @return the parsed file
//     * @throws ClientException if this target is a directory
//     */
//    public CompilationUnit toCompilationUnit(Autograder autograder) {
//        if (toFile().isDirectory()) {
//            throw new ClientException("Cannot parse directory " + toPathString());
//        }
//        return Parser.parse(autograder.getJavaLevel(), toFile());
//    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Target other) {
            return this.toPath().equals(other.toPath()) &&
                    this.toFile().equals(other.toFile()) &&
                    this.toDirectory().equals(other.toDirectory());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
