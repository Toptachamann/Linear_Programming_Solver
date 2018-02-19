#LPSolver
====================
A bunch of classes for dealing with linear programming.

###Description
====================
LPSolver - main class for solving linear programs. Accepts objects of LPStandardForm, returns optimal objective function value.
LPInputReader - a class for reading lp from a source using several assumptions about the input,
i.e. min/max is specified in the first line (Min/Max works as well), objective function is specified in the second
line of a source and all constraints are specified further one per line. For example:

max  
2x1 + 3.05*x3  
1.05*x4 + 25*x1 == 0  
3.66x1 = 3  
2x2 + x3 <= 0  
x1 + x2 + x3 + x24 >= 0  

is a valid linear programme.
An auxiliary class called LPStandardForm is used to hold all important data about linear program.
LPSolver is a main class. It has several options for supplying it with desired rounding mode, epsilon and INF
parameters as well as different possibilities for printing solution progress to the specified resource (or not).
Its objects are initialized with LPStandardForm instance. Method solve() returns optimal objective function value or
throws an exception, if this linear program is infeasible or unbounded.
A variable name is a sequence of letters (capitalized or not) with an optional index at the end and without spaces.
For example, x, y, x1, t125, var, var125 are considered a valid variable names.
As always, feel free to fork, collaborate, pull requests or report bugs.

###Changes
========================
* In the version 1.1 several bugs were fixed and the way of reading and writing data to external sources
was changed in order to improve performance (cause if the number of variables is greater than 10K, running time leaves much to
be desires).
* In the new update multithreaded feature was added to LPSolver. When the size of a linear program exceeds some certain bound, LPSolver begins to perform paralel pivots. 

