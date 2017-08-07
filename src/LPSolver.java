import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;

public class LPSolver {
    private LPStandardForm stForm;

    private ArrayList<ArrayList<BigDecimal>> A;
    private ArrayList<BigDecimal> b, c;
    private HashMap<Integer, String> variables;
    private HashMap<String, Integer> initialCoefficients;
    private HashMap<String, Integer> coefficients;
    private ArrayList<String> initialVariables, slackVariables;
    private int n, m;
    private BigDecimal v;

    private MathContext rounder, printRounder;
    private BigDecimal epsilon;
    private BigDecimal INF;

    private BufferedWriter out;

    private static class LPState {
        ArrayList<ArrayList<BigDecimal>> A;
        ArrayList<BigDecimal> b, c;
        BigDecimal v;
        HashMap<Integer, String> variables;
        HashMap<String, Integer> coefficients;

        public LPState(ArrayList<ArrayList<BigDecimal>> A, ArrayList<BigDecimal> b, ArrayList<BigDecimal> c, BigDecimal v,
                       HashMap<Integer, String> variables, HashMap<String, Integer> coefficients) {
            this.A = A;
            this.b = b;
            this.c = c;
            this.variables = variables;
            this.coefficients = coefficients;
            this.v = v;
        }
    }

    public LPSolver(LPStandardForm stForm, MathContext rounder, MathContext printRounder, BigDecimal epsilon, BigDecimal INF, String outPath) throws IOException {
        this.stForm = stForm;
        initialize(stForm);

        this.INF = INF;
        this.epsilon = epsilon;
        this.rounder = rounder;
        this.printRounder = printRounder;

        setOut(outPath);
    }

    public LPSolver(LPStandardForm stForm, MathContext rounder, MathContext printRounder, BigDecimal epsilon, BigDecimal INF) {
        this.stForm = stForm;
        initialize(stForm);

        this.INF = INF;
        this.epsilon = epsilon;
        this.rounder = rounder;
        this.printRounder = printRounder;

        this.out = null;
    }

    public LPSolver(LPStandardForm stFrom, String outPath) throws IOException {
        this.stForm = stFrom;
        initialize(stFrom);

        this.out = null;

        this.INF = new BigDecimal("1e50");
        this.epsilon = new BigDecimal("1e-9");
        this.rounder = new MathContext(12, RoundingMode.HALF_UP);
        this.printRounder = new MathContext(3, RoundingMode.HALF_UP);

        setOut(outPath);
    }

    public LPSolver(LPStandardForm stFrom) {
        this.stForm = stFrom;
        initialize(stFrom);

        this.out = null;

        this.INF = new BigDecimal("1e50");
        this.epsilon = new BigDecimal("1e-9");
        this.rounder = new MathContext(12, RoundingMode.HALF_UP);
        this.printRounder = new MathContext(3, RoundingMode.HALF_UP);
    }

    private void initialize(LPStandardForm stForm) {
        this.A = (ArrayList<ArrayList<BigDecimal>>) stForm.getA().clone();
        this.b = (ArrayList<BigDecimal>) stForm.getb().clone();
        this.c = null;  // gets value in initialize simplex
        this.variables = (HashMap<Integer, String>) stForm.getVariables().clone();
        this.initialCoefficients = (HashMap<String, Integer>) stForm.getCoefs().clone();
        this.coefficients = (HashMap<String, Integer>) stForm.getCoefs().clone();
        this.n = stForm.getNumOfVariables();
        this.m = stForm.getNumOfInequalities();
        this.v = BigDecimal.ZERO;
        this.initialVariables = new ArrayList(variables.values());
        this.slackVariables = null;  // initialized in method convert into slack form
    }

    public void setOut(String path) throws IOException {
        out = new BufferedWriter(new FileWriter(new File(path)));
    }

    public BigDecimal solve() throws IOException, SolutionException {
        return simplex(-1);
    }

    public BigDecimal solve(int iterationInterval) throws SolutionException, IOException {
        if(iterationInterval < 1){
            throw new SolutionException("Wrong iteration Interval");
        }else {
            return simplex(iterationInterval);
        }
    }

    private BigDecimal simplex(int iterationInterval) throws SolutionException, IOException {
        printLinearProgramme();
        initializeSimplex();
        int entering, leaving, iterationCount = 0;
        while ((entering = getEntering(0)) != -1) {
            ++iterationCount;
            leaving = getLeaving(entering);
            if (leaving == -1) {
                throw new SolutionException("This linear programme is unbounded");
            }
            int initialEntering = entering, initialLeaving = leaving;
            while (leaving != -1 && b.get(leaving).abs().compareTo(epsilon) < 0) {
                entering = getEntering(entering + 1);
                if (entering == -1) {
                    break;
                }
                leaving = getLeaving(entering);
            }
            if (entering == -1 || leaving == -1) {
                pivot(initialEntering, initialLeaving);
            } else {
                pivot(entering, leaving);
            }
            if (iterationInterval > 0 && iterationCount % iterationInterval == 0) {
                System.out.println("Current objective function value is " + this.v.toPlainString() + ", number of iterations " + iterationCount);
            }
        }
        printSolution();
        System.out.println("This linear programme has optimal objective function value: " + this.v.toPlainString() + ", number of iterations: " + iterationCount);
        return this.v;
    }

    private void pivot(int entering, int leaving) {
        LPState state = LPSolver.pivot(A, b, c, v, variables, coefficients, rounder, n, m, entering, leaving);
        this.A = state.A;
        this.b = state.b;
        this.c = state.c;
        this.v = state.v;
        this.variables = state.variables;
        this.coefficients = state.coefficients;
    }

    private static LPState pivot(ArrayList<ArrayList<BigDecimal>> A, ArrayList<BigDecimal> b, ArrayList<BigDecimal> c, BigDecimal v,
                                 HashMap<Integer, String> variables, HashMap<String, Integer> coefficients, MathContext rounder, int n, int m, int entering, int leaving) {
        // Computing initialCoefficients for a new basis variable
        ArrayList<BigDecimal> pivotRow = A.get(leaving);
        BigDecimal pivotEnteringCoefficient = pivotRow.get(entering);
        b.set(leaving, b.get(leaving).divide(pivotEnteringCoefficient, rounder));
        for (int i = 0; i < n; i++) {
            if (i == entering) {
                continue;
            }
            pivotRow.set(i, pivotRow.get(i).divide(pivotEnteringCoefficient, rounder));
        }
        pivotRow.set(entering, BigDecimal.ONE.divide(pivotRow.get(entering), rounder));
        // Computing initialCoefficients in other equations
        BigDecimal newPivotEnteringCoefficient = pivotRow.get(entering);
        BigDecimal bEntering = b.get(leaving);
        for (int i = 0; i < m; i++) {
            if (i == leaving) {
                continue;
            }
            ArrayList<BigDecimal> row = A.get(i);
            BigDecimal enteringCoefficient = row.get(entering);
            b.set(i, b.get(i).subtract(enteringCoefficient.multiply(bEntering, rounder), rounder));
            row.set(entering, enteringCoefficient.multiply(newPivotEnteringCoefficient, rounder).negate());  //changed
            for (int j = 0; j < n; j++) {
                if (j == entering) {
                    continue;
                }
                row.set(j, row.get(j).subtract(enteringCoefficient.multiply(pivotRow.get(j), rounder), rounder));
            }
        }
        // computing new objective function
        BigDecimal pivotCoefficientInC = c.get(entering);
        v = v.add(b.get(leaving).multiply(pivotCoefficientInC, rounder), rounder);
        c.set(entering, pivotCoefficientInC.multiply(newPivotEnteringCoefficient).negate());
        for (int i = 0; i < n; i++) {
            if (i == entering) {
                continue;
            }
            c.set(i, c.get(i).subtract(pivotCoefficientInC.multiply(pivotRow.get(i), rounder), rounder));
        }
        // updating hash tables for variables' names and their initialCoefficients
        exchangeIndexes(variables, coefficients, n, entering, leaving);
        return new LPState(A, b, c, v, variables, coefficients);
    }

    private static void exchangeIndexes(HashMap<Integer, String> variables, HashMap<String, Integer> coefficients, int n, int entering, int leaving) {
        String enteringVarName = variables.get(entering), leavingVarName = variables.get(leaving + n);
        variables.remove(entering);
        variables.remove(n + leaving);
        coefficients.remove(enteringVarName);
        coefficients.remove(leavingVarName);

        variables.put(entering, leavingVarName);
        variables.put(leaving + n, enteringVarName);
        coefficients.put(enteringVarName, leaving + n);
        coefficients.put(leavingVarName, entering);
    }


    private void initializeSimplex() throws SolutionException, IOException {
        if (n == 0 || m == 0) {
            throw new SolutionException("Can't initialize simplex, lp is incorrect");
        }
        int indexOfMinInB = findIndexOfMinInB();
        if (indexOfMinInB == -1) {
            throw new SolutionException("Can't find minimum element in vector b");
        }
        if (b.get(indexOfMinInB).compareTo(BigDecimal.ZERO) < 0) {  //Basic solution is infeasible
            String x0Identifier = convertIntoLaux();
            this.n += 1;
            int indexOfX0 = n - 1;
            pivot(indexOfX0, indexOfMinInB);
            solveLaux();
            handleInitialization(x0Identifier);
            this.n -= 1;
        } else {
            c = (ArrayList<BigDecimal>) this.stForm.getc().clone();
            convertIntoSlackForm("");
        }
    }

    private int findIndexOfMinInB() {
        BigDecimal minInB = INF;
        int indexOfMinInB = -1;
        for (int i = 0; i < m; i++) {
            if (minInB.compareTo(b.get(i)) > 0) {
                minInB = b.get(i);
                indexOfMinInB = i;
            }
        }
        return indexOfMinInB;
    }

    private String convertIntoLaux() throws IOException {
        BigDecimal x0Coefficient = BigDecimal.ONE.negate();
        for (int i = 0; i < m; i++) {
            A.get(i).add(x0Coefficient);
        }
        c = new ArrayList<>();
        BigDecimal variablesCoefficient = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            c.add(variablesCoefficient);
        }
        c.add(x0Coefficient);
        int x0Position = n;
        String x0Identifier = "x0";
        variables.put(x0Position, x0Identifier);
        coefficients.put(x0Identifier, x0Position);
        convertIntoSlackForm(x0Identifier);
        return x0Identifier;
    }

    private void solveLaux() throws IOException, SolutionException {
        for (int i = 0; ; i++) {
            int entering = getEntering(0);
            if (entering == -1) {
                break;
            }
            int leaving = getLeaving(entering);
            if (leaving == -1) {
                throw new SolutionException("Can't find leaving on the " + String.valueOf(i + 1) + " iteration (solve L auxiliary)");
            }
            pivot(entering, leaving);
        }
    }


    private void handleInitialization(String x0Identifier) throws SolutionException, IOException {
        if (coefficients.containsKey(x0Identifier)) {
            ArrayList<BigDecimal> initialC = stForm.getc();
            int indexOfx0 = coefficients.get(x0Identifier);
            BigDecimal x0Value = (indexOfx0 < n) ? BigDecimal.ZERO : b.get(indexOfx0 - n);
            if (x0Value.abs().compareTo(epsilon) > 0) {
                throw new SolutionException("This linear programme is infeasible");
            }
            if (indexOfx0 >= n) { //x0 is basis variable
                indexOfx0 = performDegeneratePivot(indexOfx0);
            }
            restoreInitialLP(indexOfx0, x0Identifier, initialC);

        } else {
            throw new SolutionException("No such variable x0 " + x0Identifier);
        }
    }

    private int performDegeneratePivot(int indexOfx0) throws SolutionException, IOException {
        int entering = -1;
        ArrayList<BigDecimal> row = A.get(indexOfx0);
        for (int i = 0; i < n; i++) {
            if (row.get(i).compareTo(epsilon) > 0) {
                entering = i;
                break;
            }
        }
        if (entering == -1) {
            throw new SolutionException("Can't perform degenerate pivot");
        }
        pivot(entering, indexOfx0 - n);
        indexOfx0 = entering;
        return indexOfx0;
    }
    //need to refactor
    private void restoreInitialLP(int indexOfx0, String x0Identifier, ArrayList<BigDecimal> initialC) {
        for (int i = 0; i < n; i++) {
            c.set(i, BigDecimal.ZERO);
        }
        v = BigDecimal.ZERO;
        for (int i = 0; i < n - 1; i++) {
            String varToPutInC = initialVariables.get(i);
            int currentIndex = coefficients.get(varToPutInC);
            if (currentIndex >= n) {
                v = v.add(b.get(currentIndex - n));
                ArrayList<BigDecimal> row = A.get(currentIndex - n);
                for (int j = 0; j < n; j++) {
                    String varj = variables.get(j);
                    int index = coefficients.get(varj);
                    c.set(index, c.get(index).add(row.get(j), rounder));
                }
            } else {
                c.set(i, c.get(i).add(initialC.get(i), rounder));
            }
        }
        for (int i = 0; i < m; i++) {
            A.get(i).remove(indexOfx0);
        }
        c.remove(indexOfx0);
        variables.remove(indexOfx0);
        coefficients.remove(x0Identifier);
        for (int i = indexOfx0 + 1; i < n + m; i++) {
            String vari = variables.get(i);
            variables.put(i - 1, vari);
            coefficients.put(vari, i - 1);
        }
        variables.remove(n + m - 1);
    }


    private void convertIntoSlackForm(String x0) {
        slackVariables = new ArrayList<>();
        int addedVariables = 0, variableIndexCounter = 1;
        while (addedVariables < m) {
            String varName = "x" + String.valueOf(variableIndexCounter);
            if (!initialCoefficients.containsKey(varName) && varName.compareTo(x0) != 0) {
                variables.put(n + addedVariables, varName);
                initialCoefficients.put(varName, addedVariables + n - 1);
                coefficients.put(varName, n + addedVariables);

                slackVariables.add(varName);
                ++addedVariables;
            }
            ++variableIndexCounter;
        }
    }

    public int getEntering(int startPoint) {
        int positiveInC = -1;
        for (int i = startPoint; i < n; i++) {
            if (c.get(i).compareTo(epsilon) > 0) {
                positiveInC = i;
                break;
            }
        }
        return positiveInC;
    }


    public int getLeaving(int entering) throws SolutionException {
        if (entering >= 0 && entering < n) {
            int leaving = -1;
            BigDecimal minSlack = this.INF;
            for (int i = 0; i < m; i++) {
                BigDecimal aie = A.get(i).get(entering);
                BigDecimal slack;
                if (aie.compareTo(epsilon) < 0) {
                    slack = INF;
                } else {
                    slack = b.get(i).divide(aie, rounder);
                }
                if (slack.compareTo(minSlack) < 0) {
                    minSlack = slack;
                    leaving = i;
                }
            }
            return leaving;

        } else {
            throw new SolutionException("Wrong entering " + String.valueOf(entering));
        }
    }

    private void printStatement(String statement) throws IOException {
        if (out != null) {
            out.write(statement);
        }
    }

    public void printLinearProgramme() throws IOException {
        if (out != null) {
            this.stForm.printLP(out);
        }
    }

    private void printSolution() throws IOException {
        ArrayList<BigDecimal> solution = new ArrayList(n);
        for (int i = 0; i < n; i++) {
            solution.add(BigDecimal.ZERO);
        }
        for (int i = n; i < n + m; i++) {
            String varName = variables.get(i);
            int coefficient = initialCoefficients.get(varName);
            if (coefficient < n) {
                solution.set(coefficient, b.get(i - n));
            }
        }
        printStatement("Optimal objective value: " + v.setScale(printRounder.getPrecision(), printRounder.getRoundingMode()).toPlainString() + "\n");
        printStatement("Solution " + "\n");
        for (int i = 0; i < n; i++) {
            printStatement(initialVariables.get(i) + " = " + solution.get(i).setScale(printRounder.getPrecision(), printRounder.getRoundingMode()).toPlainString() + "\n");
        }
        printStatement("\n\n");
    }

    public String toString() {
        DecimalFormat formatter = new DecimalFormat("+#,##0.00;-#");
        StringBuilder builder = new StringBuilder();
        builder.append("Objective f(");
        for (int i = 0; i < n; i++) {
            builder.append(variables.get(i) + ",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(") = " + v.round(printRounder).toPlainString() + " ");
        for (int i = 0; i < n; i++) {
            BigDecimal coefficient = c.get(i);
            if (coefficient.abs().compareTo(epsilon) > 0) {
                if (coefficient.abs().compareTo(BigDecimal.ONE) != 0) {
                    builder.append(formatter.format(coefficient.round(printRounder)) + "*" + variables.get(i) + " ");
                } else if (coefficient.signum() == 1) {
                    builder.append(variables.get(i) + " ");
                } else {
                    builder.append("-" + variables.get(i) + " ");
                }
            }
        }
        builder.append("\nSubject to\n");
        for (int i = 0; i < m; i++) {
            builder.append(variables.get(n + i) + " = " + b.get(i).round(printRounder).toPlainString() + " ");
            ArrayList<BigDecimal> row = A.get(i);
            for (int j = 0; j < n; j++) {
                if (row.get(j).abs().compareTo(epsilon) > 0) {
                    builder.append(formatter.format(row.get(j).round(printRounder)) + "*" + variables.get(j) + " ");
                }

            }
            builder.append("\n");
        }
        for (String var : initialVariables) {
            builder.append(var + ", ");
        }
        for (String var : slackVariables) {
            builder.append(var + ", ");
        }
        builder.deleteCharAt(builder.length() - 2);
        builder.append(">= 0\n");
        return builder.toString();
    }

    public BigDecimal getOptimalObjectiveValue() {
        return this.v;
    }
}