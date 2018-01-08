package lpsolver;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LPStandardForm {
  BigDecimal[][] A;
  BigDecimal[] b, c;
  HashMap<String, Integer> coefficients; // (variable name : coefficient in the system)
  HashMap<Integer, String> variables;
  int m, n;
  boolean maximize;

  public LPStandardForm(
      BigDecimal[][] A,
      BigDecimal[] b,
      BigDecimal[] c,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefficients,
      int m,
      int n,
      boolean maximize) {
    this(A, b, c, m, n, maximize);
    this.coefficients = coefficients;
    this.variables = variables;
  }

  @SuppressWarnings("unchecked")
  public LPStandardForm(
      ArrayList<ArrayList<BigDecimal>> A,
      ArrayList<BigDecimal> b,
      ArrayList<BigDecimal> c,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefficients,
      int m,
      int n,
      boolean maximize) {
    this.A = new BigDecimal[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        this.A[i][j] = A.get(i).get(j);
      }
    }
    this.b = b.toArray(new BigDecimal[b.size()]);
    this.c = c.toArray(new BigDecimal[c.size()]);
    this.coefficients = coefficients;
    this.variables = variables;
    this.m = m;
    this.n = n;
    this.maximize = maximize;
  }

  public LPStandardForm(
      BigDecimal[][] A, BigDecimal[] b, BigDecimal[] c, int m, int n, boolean maximize) {
    this.A = A;
    this.b = b;
    this.c = c;
    this.m = m;
    this.n = n;
    this.maximize = maximize;
  }

  public void printLP(Writer out) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder
        .append("Linear programme has following structure:\n")
        .append(maximize ? "Maximize " : "Minimize  ")
        .append("the objective function f(");
    for (Map.Entry<Integer, String> var : variables.entrySet()) {
      builder.append(var.getValue()).append(',');
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append(") = ");
    out.write(builder.toString());
    builder.setLength(0);
    appendExpression(builder, c);
    builder.append("\nSubject to\n");
    out.write(builder.toString());
    builder.setLength(0);
    for (int i = 0; i < n; i++) {
      appendExpression(builder, A[i]);
      builder.append(" <= ").append(b[i].toPlainString()).append('\n');
      out.write(builder.toString());
      builder.setLength(0);
    }
    for (Map.Entry<Integer, String> var : variables.entrySet()) {
      builder.append(var.getValue() + ',');
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append(" >= 0\n");
    out.write(builder.toString());
    out.write("\n\n");
    builder.setLength(0);
  }

  private void appendExpression(StringBuilder builder, BigDecimal[] expr) {
    int initialLength = builder.length();
    for (int i = 0; i < expr.length; i++) {
      BigDecimal coefficient = expr[i];
      if (coefficient.signum() == -1) {
        if (coefficient.abs().compareTo(BigDecimal.ONE) != 0) {
          builder.append(" - " + coefficient.abs().toString() + '*' + variables.get(i));
        } else {
          builder.append(" - " + variables.get(i));
        }

      } else if (coefficient.signum() == 1) {
        if (coefficient.compareTo(BigDecimal.ONE) != 0) {
          builder.append(" + " + expr[i].abs().toString() + '*' + variables.get(i));
        } else {
          builder.append(" + " + variables.get(i));
        }
      }
    }
    if (builder.length() > 0) {
      if (builder.charAt(initialLength + 1) != '-') {
        builder.delete(initialLength, initialLength + 3);
      } else {
        builder.deleteCharAt(initialLength);
        builder.deleteCharAt(initialLength + 1);
      }
    }
  }

  public LPStandardForm getDual() {
    if (A != null) {
      BigDecimal[][] B = new BigDecimal[n][m];
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
          B[i][j] = A[j][i];
        }
      }
      if (hasVariableNames()) {
        HashMap<Integer, String> variables = new HashMap<>(n);
        HashMap<String, Integer> coefficients = new HashMap<>(m);
        String x = "x";
        for (int i = 1; i <= n; i++) {
          variables.put(i - 1, x + i);
          coefficients.put(x + i, i - 1);
        }
        return new LPStandardForm(B, c, b, variables, coefficients, n, m, !maximize);
      } else {
        return new LPStandardForm(B, c, b, n, m, !maximize);
      }
    } else {
      return null;
    }
  }

  public boolean hasVariableNames() {
    return variables != null && coefficients != null;
  }
}
