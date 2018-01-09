package lpsolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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

  public static void main(String[] args) {
    LPInputReader reader = new LPInputReader();
    try {
      LPStandardForm standardForm =
          reader.readLP(
              new File(
                  "C:\\Java_Projects\\Combinatorial_Optimization\\MaximumFlow\\src\\MaximumFlowAsLinearProgramme.txt"));
      LPSolver solver = new LPSolver();
      long time1 = System.nanoTime();
      BigDecimal result = solver.solve(standardForm);
      long time2 = System.nanoTime();
      System.out.println((time2 - time1)/1000000000 + "seconds passed");
//      System.out.println(result);
    } catch (LPException | IOException e) {
      e.printStackTrace();
    }
  }

  public BigDecimal solve(LPStandardForm stForm) throws LPException {
    logger.trace("Start solving linear program {}", stForm);
    if (stForm.maximize) {
      BigDecimal result = simplex(stForm);
      logger.info("Optimal objective function value is {}", result);
      return result;
    } else {
      logger.trace("Converting into maximization problem");
      BigDecimal[] c = stForm.c;
      for (int i = 0; i < c.length; i++) {
        c[i] = c[i].negate();
      }
      BigDecimal result = simplex(stForm).negate();
      logger.info("Optimal objective function value is {}", result);
      return result;
    }
  }

  private BigDecimal simplex(LPStandardForm stForm) throws LPException {
    logger.trace("Starting simplex");
    LPState lpState = initializeSimplex(stForm);
    int entering, leaving;
    int numberOfIterationgs = 0;
    while ((entering = lpState.getEntering()) != -1) {
      leaving = lpState.getLeaving(entering);
      if (leaving == -1) {
        logger.error("This linear program is unbounded");
        throw new SolutionException("This linear program is unbounded");
      }
      lpState.pivot(entering, leaving);
      ++numberOfIterationgs;
      if (numberOfIterationgs % 10 == 0) {
        logger.info("Number of iterations is {}", numberOfIterationgs);
      }
    }
    return lpState.v.setScale(6, RoundingMode.HALF_UP);
  }

  private LPState initializeSimplex(LPStandardForm standardForm) throws LPException {
    logger.trace("Starting simplex initialization");
    int minInB = minInB(standardForm.b);
    if (minInB == -1 || standardForm.b[minInB].compareTo(BigDecimal.ZERO) >= 0) {
      logger.info("Basic solution is feasible");
      return convertIntoSlackForm(standardForm);
    } else {
      logger.info("Basic solution is infeasible");
      if (!standardForm.hasVariableNames()) {
        logger.trace("Standard form has no variable names, need to add default");
        addDefaultVariables(standardForm);
      }
      LPState auxLP = convertIntoAuxLP(standardForm);
      int indexOfx0 = auxLP.n - 1;
      int x0CurrentIndex = solveAuxLP(auxLP, indexOfx0, minInB);
      return handleInitialization(auxLP, standardForm, x0CurrentIndex);
    }
  }

  private int solveAuxLP(LPState auxLP, int indexOfx0, int minInB) throws SolutionException {
    logger.trace("Solving auxiliary linear program");
    int n = auxLP.n;
    auxLP.pivot(indexOfx0, minInB);
    int x0CurrentIndex = minInB + n;
    int numberOfIterations = 0;
    for (; ; ) {
      int entering = auxLP.getEntering();
      if (entering == -1) {
        break;
      }
      int leaving = auxLP.getLeaving(entering);
      if (leaving == -1) {
        logger.error("Auxiliary linear program is unbounded, something went really wrong");
        throw new SolutionException("Auxiliary lp is unbounded");
      }
      if (entering == x0CurrentIndex) {
        x0CurrentIndex = leaving + n;
      } else if (leaving + n == x0CurrentIndex) {
        x0CurrentIndex = entering;
      }
      auxLP.pivot(entering, leaving);
      ++numberOfIterations;
      if (numberOfIterations % 10 == 0) {
        logger.info("Number of iterations if {}", numberOfIterations);
      }
    }
    logger.trace("Index of auxiliary variable after solving aux. lp is {}", x0CurrentIndex);
    return x0CurrentIndex;
  }

  private LPState handleInitialization(LPState auxLP, LPStandardForm initial, int currentIndexOfX0)
      throws LPException {

    BigDecimal x0Value =
        (currentIndexOfX0 < auxLP.n) ? BigDecimal.ZERO : auxLP.b[currentIndexOfX0 - auxLP.n];
    if (x0Value.abs().compareTo(epsilon) > 0) {
      logger.error("This linear program is infeasible");
      throw new LPException("This linear program is infeasible");
    }
    if (currentIndexOfX0 >= auxLP.n) { // x0 is basis variable
      logger.trace("Auxiliary variables is basis variables, need to perform degenerate pivot");
      currentIndexOfX0 = performDegeneratePivot(auxLP, currentIndexOfX0);
    }
    return restoreInitialLP(auxLP, initial, currentIndexOfX0);
  }

  private int performDegeneratePivot(LPState auxLP, int indexOfx0) throws SolutionException {
    logger.trace("Performing degenerate pivot");
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
    logger.trace("Restoring initial linear program after initialization");
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
    logger.trace("Converting into slack form");
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
      logger.trace("This standard form has no variable names, no need to convert into slack form");
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
    logger.trace("Converting into auxiliary linear program");
    int m = standardForm.m;
    int n = standardForm.n;
    BigDecimal[][] A = standardForm.A;
    BigDecimal[][] auxA = new BigDecimal[m][n + 1];
    BigDecimal x0Coeff = BigDecimal.ONE.negate();

    for (int i = 0; i < m; i++) {
      System.arraycopy(A[i], 0, auxA[i], 0, n);
      auxA[i][n] = x0Coeff;
    }

    BigDecimal[] b = new BigDecimal[standardForm.m];
    System.arraycopy(standardForm.b, 0, b, 0, standardForm.m);

    BigDecimal[] auxC = new BigDecimal[n + 1];
    Arrays.fill(auxC, BigDecimal.ZERO);
    auxC[n] = x0Coeff;

    HashMap<Integer, String> variables = new HashMap<>(standardForm.variables);
    HashMap<String, Integer> coefficients = new HashMap<>(standardForm.coefficients);

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
    logger.trace("Getting name for auxiliary variable");
    if (!coefficients.containsKey("x0")) {
      logger.trace("Name for auxiliary variable is x0");
      return "x0";
    } else {
      String var = "auxVar";
      if (!coefficients.containsKey(var)) {
        logger.trace("Name for auxiliary variable is auxVar");
        return var;
      } else {
        int i = 1;
        while (coefficients.containsKey(var + i)) {
          ++i;
        }
        logger.trace("Name for auxiliary variable is {}", var + i);
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
    logger.trace("Finding minimum element in b");
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

  private void addDefaultVariables(LPStandardForm stForm) {
    logger.trace("Adding default variables");
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
