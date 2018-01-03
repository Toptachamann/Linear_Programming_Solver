package lpsolver;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class LPState {
  public static final MathContext DEF_ROUNDER = new MathContext(12, RoundingMode.HALF_UP);
  public static final MathContext DEF_PRINT_ROUNDER = new MathContext(4, RoundingMode.HALF_UP);
  public static final BigDecimal DEF_EPSILON = new BigDecimal(BigInteger.ONE, -9);
  public static final BigDecimal DEF_INF = new BigDecimal(BigInteger.ONE, 50);
  private static final int THREAD_AMOUNT = 4;
  BigDecimal[][] A;
  BigDecimal[] b, c;
  HashMap<Integer, String> variables;
  HashMap<String, Integer> coefficients;
  BigDecimal v;
  int m, n;
  MathContext printRounder;
  MathContext rounder;
  BigDecimal epsilon;
  BigDecimal INF;

  private ThreadPoolExecutor executor;

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
    this.A = A;
    this.b = b;
    this.c = c;
    this.variables = variables;
    this.coefficients = coefficients;
    this.v = v;
    this.m = m;
    this.n = n;
    this.printRounder = printRounder;
    this.rounder = rounder;
    this.epsilon = epsilon;
    this.INF = INF;
  }

  LPState(BigDecimal[][] A,
          BigDecimal[] b,
          BigDecimal[] c,
          BigDecimal v,
          HashMap<Integer, String> variables,
          HashMap<String, Integer> coefficients,
          int m,
          int n){
    this.A = A;
    this.b = b;
    this.c = c;
    this.variables = variables;
    this.coefficients = coefficients;
    this.v = v;
    this.m = m;
    this.n = n;
    this.printRounder = DEF_PRINT_ROUNDER;
    this.rounder = DEF_ROUNDER;
    this.epsilon = DEF_EPSILON;
    this.INF = DEF_INF;
  }


  private static String getNameForX0(HashMap<String, Integer> coefficients) {
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

  @SuppressWarnings("unchecked")
  public static Pair<LPState, String> convertIntoAuxLP(LPStandardForm standardForm) {
    LPState initial = LPState.convertIntoSlackForm(standardForm);
    BigDecimal[][] A = initial.A;
    BigDecimal[] b = initial.b;
    int m = initial.m;
    int n = initial.n;
    BigDecimal[][] auxA = new BigDecimal[m][n + 1];
    BigDecimal x0Coeff = BigDecimal.ONE.negate();
    for (int i = 0; i < m; i++) {
      System.arraycopy(A[i], 0, auxA[i], 0, n);
      auxA[i][n] = x0Coeff;
    }

    BigDecimal[] auxB = b.clone();

    BigDecimal[] auxC = (BigDecimal[]) Collections.nCopies(n + 1, BigDecimal.ZERO).toArray();
    auxC[n] = x0Coeff;

    String x0Identifier = getNameForX0(initial.coefficients);
    HashMap<Integer, String> auxVariables = (HashMap<Integer, String>) initial.variables.clone();
    HashMap<String, Integer> auxCoefficients =
        (HashMap<String, Integer>) initial.coefficients.clone();
    auxVariables.put(n, x0Identifier);
    auxCoefficients.put(x0Identifier, n);
    return new ImmutablePair<>(
        new LPState(
            auxA,
            auxB,
            auxC,
            BigDecimal.ZERO,
            auxVariables,
            auxCoefficients,
            m,
            n + 1),
        x0Identifier);
  }

  public static LPState convertIntoSlackForm(LPStandardForm standardForm) {
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
        BigDecimal.ZERO,
        variables,
        coefficients,
        m,
        n);
  }

  public void pivot(int entering, int leaving) {}

  @SuppressWarnings("Duplicates")
  private void pivotSequentially(int entering, int leaving) {
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
    c[entering] = pivotCoefficientInC.divide(pivEntCoef).negate();
    for (int i = 0; i < n; i++) {
      if (i == entering) {
        continue;
      }
      c[i] = c[i].subtract(pivotCoefficientInC.multiply(pivotRow[i], rounder), rounder);
    }

    exchangeIndexes(entering, leaving);
  }

  private void pivotConcurrently(int entering, int leaving) {
    /*ArrayList<BigDecimal> pivotRow = A.get(leaving);
    BigDecimal newPivotEnteringCoefficient = pivotRow.get(entering);
    BigDecimal bEntering = b.get(leaving);
    CountDownLatch latch = new CountDownLatch(4);
    for (int k = 0; k < THREAD_AMOUNT; k++) {
      int from = (k * m) / THREAD_AMOUNT;
      int to = ((k + 1) * m) / THREAD_AMOUNT;
      pool.execute(
          () -> {
            for (int i = from; i < to; i++) {
              if (i == leaving) {
                continue;
              }
              ArrayList<BigDecimal> row = A.get(i);
              BigDecimal enteringCoefficient = row.get(entering);
              b.set(
                  i, b.get(i).subtract(enteringCoefficient.multiply(bEntering, rounder), rounder));
              row.set(
                  entering,
                  enteringCoefficient
                      .multiply(newPivotEnteringCoefficient, rounder)
                      .negate()); // changed
              for (int j = 0; j < n; j++) {
                if (j == entering) {
                  continue;
                }
                row.set(
                    j,
                    row.get(j)
                        .subtract(enteringCoefficient.multiply(pivotRow.get(j), rounder), rounder));
              }
            }
            latch.countDown();
          });
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new LPException("Problems with recomputing concurrently");
    }*/
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
    for (int i = 0; i < n; i++) {
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
    for (int i = 0; i < m; i++) {
      BigDecimal aie = A[i][entering];
      BigDecimal slack;
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
