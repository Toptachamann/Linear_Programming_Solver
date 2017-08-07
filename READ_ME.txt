A bunch of classes for dealing with linear programming.
LPInputReader - a class for reading lp from a source using several assumptions about the input,
i.e. objective function is specified in the first line of a source and all constraints are specified
further on per line. For example:
2x1 + 3.05*x3
1.05*x4 + 25x1 == 0
3.66x1 = 3
2x2 + x3 <= 0
x1 + x2 + x3 + x24 >= 0
is a valid linear programme.
An auxiliary class called LPStandardForm is used to hold all important data about linear programme.
LPSolver is a main class. It has several options for supplying it with desired rounding mode, epsilon and INF
parameters as well as different possibilities for printing solution progress to the specified resource (or not).
Its objects are initialized with LPStandardForm instance. Method solve() returns optimal objective function value or
throws an exception, if this linear programme is infeasible or unbounded.
Edit 1 : in the version 1.1 several bugs were fixed and the way of reading and writing data to external sources
was changed in order to improve performance (cause if the number of variables is greater than 10K, running time leaves much to
be desires).
As always, feel free to fork, collaborate, pull requests or report bugs.