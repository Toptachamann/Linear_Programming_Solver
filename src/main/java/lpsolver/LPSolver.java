package lpsolver;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class LPSolver {

  private static final Logger logger = LogManager.getLogger(LPSolver.class);

  private MathContext printRounder;
  private MathContext rounder;
  private BigDecimal epsilon;
  private BigDecimal inf;

  public LPSolver() {}

  public LPSolver(MathContext printRounder) {
    this.printRounder = printRounder;
  }

  public LPSolver(MathContext printRounder, MathContext rounder) {
    this.printRounder = printRounder;
    this.rounder = rounder;
  }

  public LPSolver(MathContext printRounder, MathContext rounder, BigDecimal epsilon) {
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
  }

  public LPSolver(
      MathContext printRounder, MathContext rounder, BigDecimal epsilon, BigDecimal inf) {
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
    this.inf = inf;
  }

  public BigDecimal simplex(LPStandardForm stForm) throws SolutionException {
    LPState lpState = initializeSimplex(stForm);
    // log printState();
    int entering, leaving, iterationCount = 0;
    while ((entering = lpState.getEntering()) != -1) {
      ++iterationCount;
      leaving = lpState.getLeaving(entering);
      if (leaving == -1) {
        // log printStatement("This linear program is unbounded\n\n");
        throw new LPException("This linear program is unbounded");
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
      Pair<LPState, String> result = convertIntoAuxLP(standardForm);
      LPState auxLP = result.getKey();
      String x0Identifier = result.getValue();
      int indexOfx0 = auxLP.getN() - 1;
      solveLaux(auxLP, indexOfx0, minInB);
      return handleInitialization(auxLP, standardForm, x0Identifier);
    }
  }

  private void solveLaux(LPState auxLP, int indexOfx0, int minInB) throws SolutionException {
    // initial pivot
    auxLP.pivot(indexOfx0, minInB);
    for (int i = 0; ; i++) {
      int entering = auxLP.getEntering();
      if (entering == -1) {
        break;
      }
      int leaving = auxLP.getLeaving(entering);
      if (leaving == -1) {
        throw logger.throwing(new SolutionException("Auxiliary lp is unbounded"));
      }
      auxLP.pivot(entering, leaving);
      // log printProgress(entering, leaving);
    }
  }

  private LPState handleInitialization(LPState auxLP, LPStandardForm initial, String x0Identifier)
      throws SolutionException {
    BigDecimal[] initialC = initial.c;
    int currentIndexOfX0 = auxLP.coefficients.get(x0Identifier);

    BigDecimal x0Value =
        (currentIndexOfX0 < auxLP.n) ? BigDecimal.ZERO : auxLP.b[currentIndexOfX0 - auxLP.n];
    if (x0Value.abs().compareTo(epsilon) > 0) {
      throw logger.throwing(new LPException("This linear program is infeasible"));
    }
    if (currentIndexOfX0 >= auxLP.n) { // x0 is basis variable
      logger.info("Performing degenerate pivot");
      currentIndexOfX0 = performDegeneratePivot(auxLP, currentIndexOfX0);
    }
    return restoreInitialLP(auxLP, initial, currentIndexOfX0, x0Identifier);
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

  // need to refactor

  private LPState restoreInitialLP(
      LPState auxLP, LPStandardForm initial, int indexOfx0, String x0Identifier) {
    BigDecimal[] c = new BigDecimal[initial.n];
    Arrays.fill(c, BigDecimal.ZERO);
    String[] initialVariables = (String[]) initial.coefficients.keySet().toArray();
    BigDecimal[] initialC = initial.c;
    BigDecimal v = BigDecimal.ZERO;

    for (int i = 0; i < initial.n; i++) {
      String varToPutInC = initialVariables[i];
      BigDecimal initialCoefficient = initialC[i];
      int currentIndex = auxLP.coefficients.get(varToPutInC);
      if (currentIndex >= auxLP.n) {
        // basis variable, need to substitute
        v = v.add(auxLP.b[currentIndex - auxLP.n].multiply(initialCoefficient, rounder), rounder);
        BigDecimal[] row = auxLP.A[currentIndex - auxLP.n];
        for (int j = 0; j < auxLP.n; j++) {
          BigDecimal varCoefficient = row[j].negate();
          c[j] = c[j].add(varCoefficient.multiply(initialCoefficient, rounder), rounder);
        }
      } else {
        // non-basis variable
        c[currentIndex] = c[currentIndex].add(initialCoefficient, rounder);
      }
    }
    return new LPState(
        auxLP.A, auxLP.b, c, v, initial.variables, initial.coefficients, initial.m, initial.n);
  }

  public LPState convertIntoSlackForm(LPStandardForm standardForm) {
    HashMap<String, Integer> coefficients = standardForm.coefficients;
    HashMap<Integer, String> variables = standardForm.variables;
    int m = standardForm.m;
    int n = standardForm.n;
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
    return new LPState(
        standardForm.A,
        standardForm.b,
        standardForm.c,
        variables,
        coefficients,
        m,
        n);
  }

  @SuppressWarnings("unchecked")
  public Pair<LPState, String> convertIntoAuxLP(LPStandardForm standardForm) {
    BigDecimal[][] A = standardForm.A;
    int m = standardForm.m;
    int n = standardForm.n;
    BigDecimal[][] auxA = new BigDecimal[m][n + 1];
    BigDecimal x0Coeff = BigDecimal.ONE.negate();

    for (int i = 0; i < m; i++) {
      System.arraycopy(A[i], 0, auxA[i], 0, n);
      auxA[i][n] = x0Coeff;
    }
    standardForm.A = auxA;

    BigDecimal[] auxC = (BigDecimal[]) Collections.nCopies(n + 1, BigDecimal.ZERO).toArray();
    auxC[n] = x0Coeff;
    standardForm.c = auxC;

    String x0Identifier = getNameForX0(standardForm.coefficients);
    standardForm.variables.put(n, x0Identifier);
    standardForm.coefficients.put(x0Identifier, n);

    return new ImmutablePair<>(convertIntoSlackForm(standardForm), x0Identifier);
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
}
