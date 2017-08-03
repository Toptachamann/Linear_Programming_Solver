
import java.math.*;
import java.util.*;

public class LPStandardForm {
    private ArrayList<ArrayList<BigDecimal>> A;
    private ArrayList<BigDecimal> b, c;
    private HashMap<String, Integer> coefficients;               //(variable name : coefficient in the system)
    private HashMap<Integer, String> variables;
    private int numOfVariables, numOfInequalities;
    private boolean minimize;

    public LPStandardForm() {
        A = new ArrayList<ArrayList<BigDecimal>>();
        b = new ArrayList<BigDecimal>();
        c = new ArrayList<BigDecimal>();
        variables = new HashMap<Integer, String>();
        coefficients = new HashMap<String, Integer>();
        numOfVariables = 0;
        numOfInequalities = 0;
        minimize = false;
    }

    public LPStandardForm(ArrayList<ArrayList<BigDecimal>> A, ArrayList<BigDecimal> b, ArrayList<BigDecimal> c,
                          HashMap<Integer, String> variables, HashMap<String, Integer> coefs, int numOfVariables, int numOfInequalities, boolean minimize) {
        this.A = A;
        this.b = b;
        this.c = c;
        this.variables = variables;
        this.numOfVariables = numOfVariables;
        this.numOfInequalities = numOfInequalities;
        this.minimize = minimize;
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

    public void setMinimize(boolean minimize) {
        this.minimize = minimize;
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

    public boolean getMinimize() {
        return this.minimize;
    }

    public String toString(String end) {
        return this.toString() + end;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Linear programme has following structure:\n" + (minimize ? "Minimize " : "Maximize ") + "the objective function f(");
        for (Map.Entry<Integer, String> var : variables.entrySet()) {
            builder.append(var.getValue() + ',');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(") = ");
        appendExpression(builder, c);
        builder.append("\nSubject to\n");
        for (int i = 0; i < A.size(); i++) {
            appendExpression(builder, A.get(i));
            builder.append(" <= " + b.get(i).toPlainString() + '\n');
        }
        for (Map.Entry<Integer, String> var : variables.entrySet()) {
            builder.append(var.getValue() + ',');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(" >= 0\n");
        return builder.toString();
    }

    private void appendExpression(StringBuilder builder, ArrayList<BigDecimal> expr) {
        for (int i = 0; i < expr.size(); i++) {
            if (expr.get(i).compareTo(BigDecimal.ZERO) < 0) {
                if (i > 0) {
                    builder.append(" - " + expr.get(i).abs().toString() + '*' + variables.get(i));

                } else {
                    builder.append("-" + expr.get(i).abs().toString() + '*' + variables.get(i));

                }
            } else {
                if (i > 0) {
                    builder.append(" + " + expr.get(i).abs().toString() + '*' + variables.get(i));
                } else {
                    builder.append(expr.get(i).abs().toString() + '*' + variables.get(i));
                }
            }
        }
    }

    public LPStandardForm getDual() {
        if (A != null) {
            int m = A.size(), n = A.get(0).size();
            ArrayList<ArrayList<BigDecimal>> B = new ArrayList<ArrayList<BigDecimal>>(n);
            for (int i = 0; i < n; i++) {
                B.add(new ArrayList<BigDecimal>(m));
            }
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    B.get(j).add(new BigDecimal(A.get(i).get(j).toString()));
                }
            }
            HashMap<Integer, String> variables = new HashMap<Integer, String>(m);
            HashMap<String, Integer> coefficients = new HashMap<String, Integer>(m);
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
