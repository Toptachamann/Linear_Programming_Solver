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
  public static final MathContext DEF_ROUNDER = new MathContext(12, RoundingMode.HALF_UP);
  public static final MathContext DEF_PRINT_ROUNDER = new MathContext(4, RoundingMode.HALF_UP);
  public static final BigDecimal DEF_EPSILON = new BigDecimal(BigInteger.ONE, 9);
  public static final BigDecimal DEF_INF = new BigDecimal(BigInteger.ONE, -50);
  public static final int THREAD_AMOUNT = 4;
  public static final int PARALLEL_THRESHOLD = 30;
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
    this.A = A;
    this.b = b;
    this.c = c;
    this.variables = variables;
    this.coefficients = coefficients;
    this.v = BigDecimal.ZERO;
    this.m = m;
    this.n = n;
    this.printRounder = DEF_PRINT_ROUNDER;
    this.rounder = DEF_ROUNDER;
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

  public static LPState convertIntoLPState(LPStandardForm standardForm) {
    return new LPState(
        standardForm.A,
        standardForm.b,
        standardForm.c,
        BigDecimal.ZERO,
        standardForm.variables,
        standardForm.coefficients,
        standardForm.m,
        standardForm.n);
  }

  public void pivot(int entering, int leaving) throws SolutionException {
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
    // recalculate leaving row
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
    }

    // computing new objective function
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
            for (int i = from; i < to; i++) {
              if (i == entering) {
                continue;
              }
              pivotRow[i] = pivotRow[i].divide(pivEntCoef, rounder);
            }
            latch1.countDown();
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
            for (int i = from; i < to; i++) {
              if (i == entering) {
                continue;
              }
              c[i] = c[i].subtract(pivotCoefficientInC.multiply(pivotRow[i], rounder), rounder);
            }
            latch3.countDown();
          });
    }
    latch3.await();
  }

  private void exchangeIndexes(int entering, int leaving) {
    String enteringVarName = variables.get(entering), leavingVarName = variables.get(leaving + n);
    variables.put(entering, leavingVarName);
    variables.put(leaving + n, enteringVarName);
    coefficients.put(enteringVarName, leaving + n);
    coefficients.put(leavingVarName, entering);
  }

  public int getEntering() {
    int positiveInC = -1;
    for (int i = 0; i < m; i++) {
      if (c[i].compareTo(epsilon) > 0) {
        positiveInC = i;
        break;
      }
    }
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

  public int getM() {
    return m;
  }

  public int getN() {
    return n;
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
