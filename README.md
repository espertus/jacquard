# Jacquard

This is a Java autograder with a planned release of
late summer 2023. While most of it is platform-independent, it will
include Gradescope integration. I would be happy to get collaborators
familiar with other systems this could hook into.

Features include:

* Syntax-based analysis using [JavaParser](https://github.com/javaparser/javaparser)
* Static analysis with [Checkstyle](https://checkstyle.org/) and
  [PMD](https://pmd.github.io/).
* Test coverage and cyclomatic complexity measurement with 
  [JaCoCo](https://www.jacoco.org/jacoco/).
* Unit testing with [JUnit 5](https://junit.org/junit5/), including:
  * running staff tests against student code
  * running student tests against
    * student code
    * intentionally buggy staff-written code
    * correct staff-written code
* [Leaderboards](https://gradescope-autograders.readthedocs.io/en/latest/leaderboards/)
