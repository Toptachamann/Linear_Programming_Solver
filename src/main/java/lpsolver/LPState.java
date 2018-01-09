package lpsolver;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LPState {
  public static final MathContext DEF_ROUNDER = new MathContext(15, RoundingMode.HALF_UP);
  public static final MathContext DEF_PRINT_ROUNDER = new MathContext(4, RoundingMode.HALF_UP);
  public static final BigDecimal DEF_EPSILON = new BigDecimal(BigInteger.ONE, 9);
  public static final BigDecimal DEF_INF = new BigDecimal(BigInteger.ONE, -50);
  public static final int THREAD_AMOUNT = 4;
  public static final int PARALLEL_THRESHOLD = 3000000;
  private static final Logger logger = LogManager.getLogger(LPState.class);
  BigDecimal[][] A;
  BigDecimal[] b, c;
  HashMap<Integer, String> variables;
  HashMap<String, Integer> coefficients;
  BigDecimal v;
  int m, n;
  private MathContext printRounder;
  private MathContext rounder;
  private BigDecimal epsilon;
  private BigDecimal INF;
  private ExecutorService pool;

  LPState(
      BigDecimal[][] A,
      BigDecimal[] b,
      BigDecimal[] c,
      BigDecimal v,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefficients,
      int m,
      int n,
      MathContext printRounder,
      MathContext rounder,
      BigDecimal epsilon,
      BigDecimal INF) {
    this(A, b, c, variables, coefficients, m, n);
    this.v = v;
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
    this.INF = INF;
  }

  LPState(
      BigDecimal[][] A,
      BigDecimal[] b,
      BigDecimal[] c,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefficients,
      int m,
      int n) {
    this(A, b, c, m, n);
    this.variables = variables;
    this.coefficients = coefficients;
  }

  LPState(
      BigDecimal[][] A,
      BigDecimal[] b,
      BigDecimal[] c,
      int m,
      int n,
      MathContext rounder,
      MathContext printRounder,
      BigDecimal epsilon,
      BigDecimal INF) {
    this(A, b, c, m, n);
    this.rounder = rounder;
    this.printRounder = printRounder;
    this.epsilon = epsilon;
    this.INF = INF;
  }

  LPState(BigDecimal[][] A, BigDecimal[] b, BigDecimal[] c, int m, int n) {
    this.A = A;
    this.b = b;
    this.c = c;
    this.v = BigDecimal.ZERO;
    this.m = m;
    this.n = n;
    this.rounder = DEF_ROUNDER;
    this.printRounder = DEF_PRINT_ROUNDER;
    this.epsilon = DEF_EPSILON;
    this.INF = DEF_INF;
  }

  LPState(
      BigDecimal[][] A,
      BigDecimal[] b,
      BigDecimal[] c,
      BigDecimal v,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefficients,
      int m,
      int n) {
    this(A, b, c, variables, coefficients, m, n);
    this.v = v;
  }

  public void pivot(int entering, int leaving) throws SolutionException {
    logger.trace(
        "Start pivoting with, entering - {}, leaving - {}",
        hasVariablesNames() ? variables.get(entering) : entering,
        hasVariablesNames() ? variables.get(leaving) : leaving);
    if (m >= PARALLEL_THRESHOLD) {
      try {
        pivotConcurrently(entering, leaving);
      } catch (InterruptedException e) {
        logger.catching(e);
        throw logger.throwing(
            new SolutionException("Some thread was interrupted during calculation", e));
      }
    } else {
      pivotSequentially(entering, leaving);
    }
  }

  @SuppressWarnings("Duplicates")
  void pivotSequentially(int entering, int leaving) {
    logger.trace("Start pivoting sequentially");
    // recalculate leaving row
    logger.trace("Recalculating leaving row");
    BigDecimal[] pivotRow = A[leaving];
    BigDecimal pivEntCoef = pivotRow[entering];
    pivotRow[entering] = BigDecimal.ONE.divide(pivEntCoef, rounder);
    for (int i = 0; i < n; i++) {
      if (i == entering) {
        continue;
      }
      pivotRow[i] = pivotRow[i].divide(pivEntCoef, rounder);
    }
    b[leaving] = b[leaving].divide(pivEntCoef, rounder);

    // recalculate other rows
    logger.trace("Recalculating other rows");
    BigDecimal bEntering = b[leaving];
    for (int i = 0; i < m; i++) {
      if (i == leaving) {
        continue;
      }
      BigDecimal[] currentRow = A[i];
      BigDecimal curEntCoef = currentRow[entering];
      currentRow[entering] = curEntCoef.divide(pivEntCoef, rounder).negate();
      for (int j = 0; j < n; j++) {
        if (j == entering) {
          continue;
        }
        currentRow[j] = currentRow[j].subtract(curEntCoef.multiply(pivotRow[j], rounder), rounder);
      }
      b[i] = b[i].subtract(curEntCoef.multiply(bEntering, rounder), rounder);
      logger.trace("Finished pivoting");
    }

    // computing new objective function
    logger.trace("Computing new objective function");
    BigDecimal pivotCoefficientInC = c[entering];
    v = v.add(b[leaving].multiply(pivotCoefficientInC, rounder), rounder);
    c[entering] = pivotCoefficientInC.divide(pivEntCoef, rounder).negate();
    for (int i = 0; i < n; i++) {
      if (i == entering) {
        continue;
      }
      c[i] = c[i].subtract(pivotCoefficientInC.multiply(pivotRow[i], rounder), rounder);
    }

    exchangeIndexes(entering, leaving);
  }

  @SuppressWarnings("Duplicates")
  void pivotConcurrently(int entering, int leaving) throws InterruptedException {
    logger.trace("Start pivoting concurrently");
    if (pool == null) {
      pool = Executors.newFixedThreadPool(THREAD_AMOUNT);
    }
    CountDownLatch latch1 = new CountDownLatch(THREAD_AMOUNT);

    // recalculate leaving row
    BigDecimal[] pivotRow = A[leaving];
    BigDecimal pivEntCoef = pivotRow[entering];
    pivotRow[entering] = BigDecimal.ONE.divide(pivEntCoef, rounder);
    for (int k = 0; k < THREAD_AMOUNT; k++) {
      int effectivelyFinalK = k;
      pool.execute(
          () -> {
            int from = (effectivelyFinalK * n) / THREAD_AMOUNT;
            int to = ((effectivelyFinalK + 1) * n) / THREAD_AMOUNT;
            logger.trace("Recalculating leaving row from {} to {}", from, to);
            for (int i = from; i < to; i++) {
              if (i == entering) {
                continue;
              }
              pivotRow[i] = pivotRow[i].divide(pivEntCoef, rounder);
            }
            latch1.countDown();
            logger.trace("Finished recalculating leaving row from {} to {}", from, to);
          });
    }
    latch1.await();
    b[leaving] = b[leaving].divide(pivEntCoef, rounder);

    CountDownLatch latch2 = new CountDownLatch(THREAD_AMOUNT);
    // recalculate other rows
    BigDecimal bEntering = b[leaving];
    for (int k = 0; k < THREAD_AMOUNT; k++) {
      int effectivelyFinalK = k;
      pool.execute(
          () -> {
            int from = (effectivelyFinalK * m) / THREAD_AMOUNT;
            int to = ((effectivelyFinalK + 1) * m) / THREAD_AMOUNT;
            logger.trace("Recalculating constraint matrix from {} to {}", from, to);
            for (int i = from; i < to; i++) {
              if (i == leaving) {
                continue;
              }
              BigDecimal[] currentRow = A[i];
              BigDecimal curEntCoef = currentRow[entering];
              currentRow[entering] = curEntCoef.divide(pivEntCoef, rounder).negate();
              for (int j = 0; j < n; j++) {
                if (j == entering) {
                  continue;
                }
                currentRow[j] =
                    currentRow[j].subtract(curEntCoef.multiply(pivotRow[j], rounder), rounder);
              }
              b[i] = b[i].subtract(curEntCoef.multiply(bEntering, rounder), rounder);
            }
            latch2.countDown();
            logger.trace("Finished recalculating constraint matrix from {} to {}", from, to);
          });
    }
    latch2.await();

    CountDownLatch latch3 = new CountDownLatch(THREAD_AMOUNT);
    // computing new objective function
    BigDecimal pivotCoefficientInC = c[entering];
    v = v.add(b[leaving].multiply(pivotCoefficientInC, rounder), rounder);
    c[entering] = pivotCoefficientInC.divide(pivEntCoef, rounder).negate();
    for (int k = 0; k < THREAD_AMOUNT; k++) {
      int effectivelyFinalK = k;
      pool.execute(
          () -> {
            int from = (effectivelyFinalK * n) / THREAD_AMOUNT;
            int to = ((effectivelyFinalK + 1) * n) / THREAD_AMOUNT;
            logger.trace("Computing new objective function from {} to {}", from, to);
            for (int i = from; i < to; i++) {
              if (i == entering) {
                continue;
              }
              c[i] = c[i].subtract(pivotCoefficientInC.multiply(pivotRow[i], rounder), rounder);
            }
            logger.trace("Finished computing new objective function from {} to {}", from, to);
            latch3.countDown();
          });
    }
    latch3.await();
    exchangeIndexes(entering, leaving);
    logger.trace("Finished concurrent pivot");
  }

  public int getEntering() {
    logger.trace("Getting entering");
    int positiveInC = -1;
    for (int i = 0; i < n; i++) {
      if (c[i].compareTo(epsilon) > 0) {
        positiveInC = i;
        break;
      }
    }
    logger.trace("Entering is {}", hasVariablesNames() ? variables.get(positiveInC) : positiveInC);
    return positiveInC;
  }

  public int getLeaving(int entering) {
    Validate.isTrue(entering >= 0 && entering < n);
    int leaving = -1;
    BigDecimal minSlack = this.INF;
    BigDecimal slack;
    for (int i = 0; i < m; i++) {
      BigDecimal aie = A[i][entering];
      if (aie.compareTo(epsilon) < 0) {
        slack = INF;
      } else {
        slack = b[i].divide(aie, rounder);
      }
      if (slack.compareTo(minSlack) < 0) {
        minSlack = slack;
        leaving = i;
      }
    }
    return leaving;
  }

  public boolean hasVariablesNames() {
    return variables != null && coefficients != null;
  }

  private void exchangeIndexes(int entering, int leaving) {
    if (hasVariablesNames()) {
      logger.trace("Exchanging indexes");
      String enteringVarName = variables.get(entering), leavingVarName = variables.get(leaving + n);
      variables.put(entering, leavingVarName);
      variables.put(leaving + n, enteringVarName);
      coefficients.put(enteringVarName, leaving + n);
      coefficients.put(leavingVarName, entering);
    }
  }

  public String toString() {
    DecimalFormat formatter = new DecimalFormat("+#,##0.00;-#");
    StringBuilder builder = new StringBuilder();
    builder.append("Objective f(");
    for (int i = 0; i < n; i++) {
      builder.append(variables.get(i));
      builder.append(",");
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append(") = ");
    builder.append(v.setScale(printRounder.getPrecision(), printRounder.getRoundingMode()));
    builder.append(" ");
    for (int i = 0; i < n; i++) {
      BigDecimal coefficient = c[i];
      if (coefficient.abs().compareTo(epsilon) > 0) {
        if (coefficient.abs().compareTo(BigDecimal.ONE) != 0) {
          builder.append(
              formatter.format(
                  coefficient.setScale(
                      printRounder.getPrecision(), printRounder.getRoundingMode())));
          builder.append("*");
          builder.append(variables.get(i));
          builder.append(" ");
        } else if (coefficient.signum() == 1) {
          builder.append(" + ");
          builder.append(variables.get(i));
          builder.append(" ");
        } else {
          builder.append(" - ");
          builder.append(variables.get(i));
          builder.append(" ");
        }
      }
    }
    builder.append("\nSubject to\n");
    for (int i = 0; i < m; i++) {
      builder
          .append(variables.get(n + i))
          .append(" = ")
          .append(
              b[i].setScale(printRounder.getPrecision(), printRounder.getRoundingMode())
                  .toPlainString())
          .append(" ");
      BigDecimal[] row = A[i];
      for (int j = 0; j < n; j++) {
        if (row[j].abs().compareTo(epsilon) > 0) {
          builder.append(
              formatter.format(
                  row[j]
                      .negate()
                      .setScale(printRounder.getPrecision(), printRounder.getRoundingMode())));
          builder.append("*");
          builder.append(variables.get(j));
          builder.append(" ");
        }
      }
      builder.append("\n");
    }
    for (String var : coefficients.keySet()) {
      builder.append(var);
      builder.append(", ");
    }
    builder.deleteCharAt(builder.length() - 2);
    builder.append(">= 0\n");
    return builder.toString();
  }
}
