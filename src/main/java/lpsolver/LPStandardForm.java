package lpsolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LPStandardForm {
  private ArrayList<ArrayList<BigDecimal>> A;
  private ArrayList<BigDecimal> b, c;
  private HashMap<String, Integer> coefficients; // (variable name : coefficient in the system)
  private HashMap<Integer, String> variables;
  private int numOfVariables, numOfInequalities;
  private boolean maximize;

  public LPStandardForm(
      ArrayList<ArrayList<BigDecimal>> A,
      ArrayList<BigDecimal> b,
      ArrayList<BigDecimal> c,
      HashMap<String, Integer> coefficients,
      HashMap<Integer, String> variables,
      int numOfVariables,
      int numOfInequalities,
      boolean maximize) {
    this.A = A;
    this.b = b;
    this.c = c;
    this.coefficients = coefficients;
    this.variables = variables;
    this.numOfVariables = numOfVariables;
    this.numOfInequalities = numOfInequalities;
    this.maximize = maximize;
  }

  public void setb(ArrayList<BigDecimal> b) {
    this.b = b;
  }

  public void setc(ArrayList<BigDecimal> c) {
    this.c = c;
  }

  public void setCoefficients(HashMap<String, Integer> coefficients) {
    this.coefficients = coefficients;
  }

  public ArrayList<ArrayList<BigDecimal>> getA() {
    return this.A;
  }

  public void setA(ArrayList<ArrayList<BigDecimal>> A) {
    this.A = A;
  }

  public ArrayList<BigDecimal> getb() {
    return this.b;
  }

  public ArrayList<BigDecimal> getc() {
    return this.c;
  }

  public int getNumOfVariables() {
    return this.numOfVariables;
  }

  public void setNumOfVariables(int numOfVariables) {
    this.numOfVariables = numOfVariables;
  }

  public int getNumOfInequalities() {
    return this.numOfInequalities;
  }

  public void setNumOfInequalities(int numOfInequalities) {
    this.numOfInequalities = numOfInequalities;
  }

  public HashMap<Integer, String> getVariables() {
    return this.variables;
  }

  public void setVariables(HashMap<Integer, String> variables) {
    this.variables = variables;
  }

  public HashMap<String, Integer> getCoefs() {
    return this.coefficients;
  }

  public boolean getMaximize() {
    return this.maximize;
  }

  public void setMaximize(boolean maximize) {
    this.maximize = maximize;
  }

  public void printLP(BufferedWriter out) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append(
        "Linear programme has following structure:\n"
            + (maximize ? "Maximize " : "Minimize  ")
            + "the objective function f(");
    for (Map.Entry<Integer, String> var : variables.entrySet()) {
      builder.append(var.getValue() + ',');
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append(") = ");
    out.write(builder.toString());
    builder.setLength(0);
    appendExpression(builder, c);
    builder.append("\nSubject to\n");
    out.write(builder.toString());
    builder.setLength(0);
    for (int i = 0; i < A.size(); i++) {
      appendExpression(builder, A.get(i));
      builder.append(" <= " + b.get(i).toPlainString() + '\n');
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

  private void appendExpression(StringBuilder builder, ArrayList<BigDecimal> expr) {
    int initialLength = builder.length();
    for (int i = 0; i < expr.size(); i++) {
      BigDecimal coefficient = expr.get(i);
      if (coefficient.signum() == -1) {
        if (coefficient.abs().compareTo(BigDecimal.ONE) != 0) {
          builder.append(" - " + coefficient.abs().toString() + '*' + variables.get(i));
        } else {
          builder.append(" - " + variables.get(i));
        }

      } else if (coefficient.signum() == 1) {
        if (coefficient.compareTo(BigDecimal.ONE) != 0) {
          builder.append(" + " + expr.get(i).abs().toString() + '*' + variables.get(i));
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
      int m = A.size(), n = A.get(0).size();
      ArrayList<ArrayList<BigDecimal>> B = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        B.add(new ArrayList<BigDecimal>(m));
      }
      for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
          B.get(j).add(new BigDecimal(A.get(i).get(j).toString()));
        }
      }
      HashMap<Integer, String> variables = new HashMap<>(m);
      HashMap<String, Integer> coefficients = new HashMap<>(m);
      String x = "x";
      for (int i = 1; i <= m; i++) {
        variables.put(i - 1, x + i);
        coefficients.put(x + i, i - 1);
      }
      return new LPStandardForm(B, c, b, coefficients, variables, m, n, false);
    } else {
      return null;
    }
  }
}
