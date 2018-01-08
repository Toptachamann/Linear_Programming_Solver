package lpsolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class LPSolver {

  private static final Logger logger = LogManager.getLogger(LPSolver.class);

  private MathContext printRounder;
  private MathContext rounder;
  private BigDecimal epsilon;
  private BigDecimal inf;

  public LPSolver() {
    printRounder = LPState.DEF_PRINT_ROUNDER;
    rounder = LPState.DEF_ROUNDER;
    epsilon = LPState.DEF_EPSILON;
    inf = LPState.DEF_INF;
  }

  public LPSolver(MathContext printRounder) {
    this.printRounder = printRounder;
    rounder = LPState.DEF_ROUNDER;
    epsilon = LPState.DEF_EPSILON;
    inf = LPState.DEF_INF;
  }

  public LPSolver(MathContext printRounder, MathContext rounder) {
    this.printRounder = printRounder;
    this.rounder = rounder;
    epsilon = LPState.DEF_EPSILON;
    inf = LPState.DEF_INF;
  }

  public LPSolver(MathContext printRounder, MathContext rounder, BigDecimal epsilon) {
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
    inf = LPState.DEF_INF;
  }

  public LPSolver(
      MathContext printRounder, MathContext rounder, BigDecimal epsilon, BigDecimal inf) {
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
    this.inf = inf;
  }

  public BigDecimal solve(LPStandardForm stForm) throws SolutionException {
    if (stForm.maximize) {
      return simplex(stForm).round(printRounder);
    } else {
      BigDecimal[] c = stForm.c;
      for (int i = 0; i < c.length; i++) {
        c[i] = c[i].negate();
      }
      return simplex(stForm).negate().round(printRounder);
    }
  }

  private BigDecimal simplex(LPStandardForm stForm) throws SolutionException {
    LPState lpState = initializeSimplex(stForm);
    // log printState();
    int entering, leaving;
    while ((entering = lpState.getEntering()) != -1) {
      leaving = lpState.getLeaving(entering);
      if (leaving == -1) {
        logger.error("This linear program is unbounded");
        throw new SolutionException("This linear program is unbounded");
      }
      lpState.pivot(entering, leaving);
      // log printProgress(entering, leaving);
    }
    // log
    /*if (iterationInterval > 0 && iterationCount % iterationInterval == 0) {
      System.out.println(
          "Current objective function value is "
              + this.v.toPlainString()
              + ", number of iterations "
              + iterationCount);
    }*/
    // log printSolution();
    /*System.out.println(
    "This linear program has optimal objective function value: "
        + this.v
            .setScale(printRounder.getPrecision(), printRounder.getRoundingMode())
            .toPlainString()
        + ", number of iterations: "
        + iterationCount);*/
    return lpState.v;
  }

  private LPState initializeSimplex(LPStandardForm standardForm) throws SolutionException {
    // log printStatement("Initializing simplex\n\n");
    int minInB = minInB(standardForm.b);
    if (minInB == -1 || standardForm.b[minInB].compareTo(BigDecimal.ZERO) >= 0) {
      logger.info("Basic solution is feasible");
      return convertIntoSlackForm(standardForm);
    } else {
      logger.info("Basic solution is infeasible");
      if (!standardForm.hasVariableNames()) {
        fillWithVariables(standardForm);
      }
      LPState auxLP = convertIntoAuxLP(standardForm);
      int indexOfx0 = auxLP.n - 1;
      int x0CurrentIndex = solveAuxLP(auxLP, indexOfx0, minInB);
      return handleInitialization(auxLP, standardForm, x0CurrentIndex);
    }
  }

  private int solveAuxLP(LPState auxLP, int indexOfx0, int minInB) throws SolutionException {
    int n = auxLP.n;
    auxLP.pivot(indexOfx0, minInB);
    int x0CurrentIndex = minInB + n;
    for (; ; ) {
      int entering = auxLP.getEntering();
      if (entering == -1) {
        break;
      }
      int leaving = auxLP.getLeaving(entering);
      if (leaving == -1) {
        throw logger.throwing(new SolutionException("Auxiliary lp is unbounded"));
      }
      if (entering == x0CurrentIndex) {
        x0CurrentIndex = leaving + n;
      } else if (leaving + n == x0CurrentIndex) {
        x0CurrentIndex = entering;
      }
      auxLP.pivot(entering, leaving);
    }
    return x0CurrentIndex;
  }

  private LPState handleInitialization(LPState auxLP, LPStandardForm initial, int currentIndexOfX0)
      throws SolutionException {

    BigDecimal x0Value =
        (currentIndexOfX0 < auxLP.n) ? BigDecimal.ZERO : auxLP.b[currentIndexOfX0 - auxLP.n];
    if (x0Value.abs().compareTo(epsilon) > 0) {
      logger.error("This linear program is infeasible");
      throw new LPException("This linear program is infeasible");
    }
    if (currentIndexOfX0 >= auxLP.n) { // x0 is basis variable
      logger.info("Performing degenerate pivot");
      currentIndexOfX0 = performDegeneratePivot(auxLP, currentIndexOfX0);
    }
    return restoreInitialLP(auxLP, initial, currentIndexOfX0);
  }

  private int performDegeneratePivot(LPState auxLP, int indexOfx0) throws SolutionException {
    int entering = -1;
    BigDecimal[] row = auxLP.A[indexOfx0 - auxLP.n];
    for (int i = 0; i < auxLP.n; i++) {
      if (row[i].abs().compareTo(epsilon) > 0) {
        entering = i;
        break;
      }
    }
    if (entering == -1) {
      throw logger.throwing(new SolutionException("Can't perform degenerate pivot"));
    }
    auxLP.pivot(entering, indexOfx0 - auxLP.n);
    // log printProgress(entering, indexOfx0 - n);
    return entering;
  }

  private LPState restoreInitialLP(LPState auxLP, LPStandardForm initial, int indexOfX0) {
    int n = initial.n;
    int m = auxLP.m;

    // restoring A
    BigDecimal[][] A = new BigDecimal[m][n];
    BigDecimal[][] auxA = auxLP.A;
    for (int i = 0; i < m; i++) {
      System.arraycopy(auxA[i], 0, A[i], 0, indexOfX0);
      System.arraycopy(auxA[i], indexOfX0 + 1, A[i], indexOfX0, n - indexOfX0);
    }

    Set<String> initialVariables = initial.coefficients.keySet();
    BigDecimal v = BigDecimal.ZERO;
    BigDecimal[] c = new BigDecimal[n];
    Arrays.fill(c, BigDecimal.ZERO);
    for (String currentVar : initialVariables) {
      int index = initial.coefficients.get(currentVar);
      BigDecimal initialCoefficient = initial.c[index];
      int currentIndex = auxLP.coefficients.get(currentVar);
      if (currentIndex >= auxLP.n) {
        // basis variable, need to substitute
        v = v.add(auxLP.b[currentIndex - auxLP.n].multiply(initialCoefficient, rounder), rounder);
        BigDecimal[] row = A[currentIndex - auxLP.n];
        for (int j = 0; j < n; j++) {
          BigDecimal varCoefficient = row[j].negate();
          c[j] = c[j].add(varCoefficient.multiply(initialCoefficient, rounder), rounder);
        }
      } else {
        // non-basis variable
        c[currentIndex] = c[currentIndex].add(initialCoefficient, rounder);
      }
    }

    HashMap<Integer, String> variables = auxLP.variables;
    HashMap<String, Integer> coefficients = auxLP.coefficients;
    String x0 = variables.get(indexOfX0);
    coefficients.remove(x0);
    for (int i = indexOfX0; i < n + m; i++) {
      String varName = variables.get(i + 1);
      variables.put(i, varName);
      coefficients.put(varName, i);
    }
    variables.remove(n + m);
    return new LPState(A, auxLP.b, c, v, variables, coefficients, initial.m, initial.n);
  }

  public LPState convertIntoSlackForm(LPStandardForm stForm) {
    if (stForm.hasVariableNames()) {
      HashMap<String, Integer> coefficients = stForm.coefficients;
      HashMap<Integer, String> variables = stForm.variables;
      int m = stForm.m;
      int n = stForm.n;
      int addedVariables = 0;
      int variableIndexCounter = 1;
      while (addedVariables < m) {
        String varName = "x" + String.valueOf(variableIndexCounter);
        if (!coefficients.containsKey(varName)) {
          variables.put(n + addedVariables, varName);
          coefficients.put(varName, n + addedVariables);

          ++addedVariables;
        }
        ++variableIndexCounter;
      }
      return new LPState(stForm.A, stForm.b, stForm.c, variables, coefficients, m, n);
    } else {
      return new LPState(stForm.A, stForm.b, stForm.c, stForm.m, stForm.n);
    }
  }

  /**
   * Converts given linear program standard form into corespondent auxiliary linear program.
   *
   * @implNote Copies all fields of standard form so that {@code standardForm} and resulting {@code
   *     LPState} are independent objects
   * @param standardForm {@code LPStandardForm} to convert into auxiliary linear program
   * @return resulting auxiliary linear program
   */
  @SuppressWarnings("unchecked")
  public LPState convertIntoAuxLP(LPStandardForm standardForm) {
    int m = standardForm.m;
    int n = standardForm.n;
    BigDecimal[][] A = standardForm.A;
    BigDecimal[][] auxA = new BigDecimal[m][n + 1];
    BigDecimal x0Coeff = BigDecimal.ONE.negate();

    for (int i = 0; i < m; i++) {
      System.arraycopy(A[i], 0, auxA[i], 0, n);
      auxA[i][n] = x0Coeff;
    }

    BigDecimal[] b = standardForm.b.clone();

    BigDecimal[] auxC = new BigDecimal[n + 1];
    Arrays.fill(auxC, BigDecimal.ZERO);
    auxC[n] = x0Coeff;

    HashMap<Integer, String> variables = (HashMap<Integer, String>) standardForm.variables.clone();
    HashMap<String, Integer> coefficients =
        (HashMap<String, Integer>) standardForm.coefficients.clone();

    String x0Identifier = getNameForX0(standardForm.coefficients);
    variables.put(n, x0Identifier);
    coefficients.put(x0Identifier, n);

    LPStandardForm auxStForm =
        new LPStandardForm(
            auxA,
            b,
            auxC,
            variables,
            coefficients,
            standardForm.m,
            standardForm.n + 1,
            standardForm.maximize);
    return convertIntoSlackForm(auxStForm);
  }

  private String getNameForX0(HashMap<String, Integer> coefficients) {
    if (!coefficients.containsKey("x0")) {
      return "x0";
    } else {
      String var = "auxVar";
      if (!coefficients.containsKey(var)) {
        return var;
      } else {
        int i = 1;
        while (coefficients.containsKey(var + i)) {
          ++i;
        }
        return var + i;
      }
    }
  }

  /*private void printSolution() throws IOException {
    ArrayList<BigDecimal> solution = new ArrayList<>(n);
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
    // need to fix;
    BigDecimal vPrint = v.setScale(printRounder.getPrecision(), printRounder.getRoundingMode());
    printStatement(
        "Optimal objective value: "
            + (stForm.getMaximize() ? vPrint.toString() : vPrint.negate().toString())
            + "\n");
    printStatement("Solution " + "\n");
    for (int i = 0; i < n; i++) {
      printStatement(
          initialVariables.get(i)
              + " = "
              + solution
                  .get(i)
                  .setScale(printRounder.getPrecision(), printRounder.getRoundingMode())
                  .toPlainString()
              + "\n");
    }
    printStatement("\n\n");
  }*/
  private int minInB(BigDecimal[] b) {
    BigDecimal minInB = LPState.DEF_INF;
    int indexOfMinInB = -1;
    for (int i = 0; i < b.length; i++) {
      if (minInB.compareTo(b[i]) > 0) {
        minInB = b[i];
        indexOfMinInB = i;
      }
    }
    return indexOfMinInB;
  }

  private void fillWithVariables(LPStandardForm stForm) {
    int n = stForm.n;
    HashMap<Integer, String> variables = new HashMap<>();
    HashMap<String, Integer> coefficients = new HashMap<>();
    for (int i = 0; i < n; i++) {
      String var = "x" + (i + 1);
      variables.put(i, var);
      coefficients.put(var, i);
    }
    stForm.variables = variables;
    stForm.coefficients = coefficients;
  }
}
