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

  public LPStandardForm() {
    A = new ArrayList<>();
    b = new ArrayList<>();
    c = new ArrayList<>();
    variables = new HashMap<>();
    coefficients = new HashMap<>();
    numOfVariables = 0;
    numOfInequalities = 0;
    maximize = true;
  }

  public LPStandardForm(
      ArrayList<ArrayList<BigDecimal>> A,
      ArrayList<BigDecimal> b,
      ArrayList<BigDecimal> c,
      HashMap<Integer, String> variables,
      HashMap<String, Integer> coefs,
      int numOfVariables,
      int numOfInequalities,
      boolean maximize) {
    this.A = A;
    this.b = b;
    this.c = c;
    this.variables = variables;
    this.numOfVariables = numOfVariables;
    this.numOfInequalities = numOfInequalities;
    this.maximize = maximize;
  }

  public void setA(ArrayList<ArrayList<BigDecimal>> A) {
    this.A = (ArrayList<ArrayList<BigDecimal>>) A.clone();
  }

  public void setb(ArrayList<BigDecimal> b) {
    this.b = (ArrayList<BigDecimal>) b.clone();
  }

  public void setc(ArrayList<BigDecimal> c) {
    this.c = (ArrayList<BigDecimal>) c.clone();
  }

  public void setNumOfVariables(int numOfVariables) {
    this.numOfVariables = numOfVariables;
  }

  public void setNumOfInequalities(int numOfInequalities) {
    this.numOfInequalities = numOfInequalities;
  }

  public void setVariables(HashMap<Integer, String> variables) {
    this.variables = variables;
  }

  public void setCoefficients(HashMap<String, Integer> coefficients) {
    this.coefficients = coefficients;
  }

  public void setMaximize(boolean maximize) {
    this.maximize = maximize;
  }

  public ArrayList<ArrayList<BigDecimal>> getA() {
    return this.A;
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

  public int getNumOfInequalities() {
    return this.numOfInequalities;
  }

  public HashMap<Integer, String> getVariables() {
    return this.variables;
  }

  public HashMap<String, Integer> getCoefs() {
    return this.coefficients;
  }

  public boolean getMaximize() {
    return this.maximize;
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
      return new LPStandardForm(B, c, b, variables, coefficients, m, n, false);
    } else {
      return null;
    }
  }
}
