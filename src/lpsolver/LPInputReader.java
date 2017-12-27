package lpsolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LPInputReader {
  private LPStandardForm stForm;
  private ArrayList<ArrayList<BigDecimal>> A;
  private ArrayList<BigDecimal> b, c;
  private HashMap<Integer, String> variables;
  private HashMap<String, Integer> coefficients;
  private int numOfVariables, numOfInequalities;
  private static final Pattern tokenPattern;
  private static final Pattern inequalitySignPattern;
  private static final Pattern constraintNumberPattern;

  static {
    tokenPattern = Pattern.compile("(([+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))");
    inequalitySignPattern = Pattern.compile("(>=|<=|=|==)");
    constraintNumberPattern = Pattern.compile("(>=|<=|=|==)\\s*(-?\\d+(\\.?\\d+)?)");
  }

  public LPInputReader() {}

  private void reload() {
    this.stForm = new LPStandardForm();
    this.A = new ArrayList<>();
    this.b = new ArrayList<>();
    this.c = new ArrayList<>();
    this.variables = new HashMap<>();
    this.coefficients = new HashMap<>();
    this.numOfVariables = 0;
    this.numOfInequalities = 0;
  }

  public void readInput(String path) throws IOException, SolutionException {
    reload();

    File file = new File(path);
    if (!file.exists()) {
      throw new IOException("Can't find input file specified");
    }
    if (!file.isFile() || !file.canRead()) {
      throw new IOException("Can't read from such file");
    }
    int counter = 0;
    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String maxOrMin = in.readLine();
      if (maxOrMin == null) {
        throw new IOException("Input file is empty");
      }
      String objective = in.readLine();
      readObjective(objective);
      if (maxOrMin.equals("min") || maxOrMin.equals("Min")) {
        for (int i = 0; i < c.size(); i++) {
          c.set(i, c.get(i).negate());
        }
      } else if (!maxOrMin.equals("Max") && !maxOrMin.equals("max")) {
        throw new SolutionException("Incorrect max/min parameter");
      }
      String constraint;
      while ((constraint = in.readLine()) != null) {
        if (constraint.compareTo("") != 0) {
          this.readConstraint(constraint);
          ++counter;
        } else if (counter > 0) {
          break;
        } else {
          throw new IOException("No constraints in the input file");
        }
      }
    }
    for (ArrayList<BigDecimal> row : A) {
      int size = row.size();
      for (int j = 0; j < numOfVariables - size; j++) {
        row.add(BigDecimal.ZERO);
      }
    }
    stForm.setA(A);
    stForm.setb(b);
    stForm.setc(c);
    stForm.setVariables(variables);
    stForm.setCoefficients(coefficients);
    stForm.setMaximize(true);
    stForm.setNumOfInequalities(this.numOfInequalities);
    stForm.setNumOfVariables(numOfVariables);
  }

  private void readObjective(String objective) throws IOException {
    if (objective == null) {
      throw new IOException("No objective in the input file");
    }
    Matcher matcher = this.tokenPattern.matcher(objective);
    String t;
    for (int i = 0; matcher.find(); i++) {
      variables.put(i, matcher.group(3));
      coefficients.put(matcher.group(3), i);
      t = matcher.group(2);
      t = t.replaceAll("\\s", "");
      if (t.compareTo("") == 0 || t.compareTo("+") == 0) {
        t = "1";
      } else if (t.compareTo("-") == 0) {
        t = "-1";
      }
      c.add(new BigDecimal(t));
    }
    numOfVariables = variables.size();
  }

  @SuppressWarnings("unchecked")
  private boolean readConstraint(String constraint) throws SolutionException {
    ArrayList<BigDecimal> coefficients = new ArrayList<>(numOfVariables + 1);
    BigDecimal constraintNumber;
    for (int i = 0; i < numOfVariables; i++) {
      coefficients.add(BigDecimal.ZERO);
    }
    Matcher tokenMatcher = this.tokenPattern.matcher(constraint);
    while (tokenMatcher.find()) {
      String varName = tokenMatcher.group(3);
      if (!this.coefficients.containsKey(varName)) {
        this.variables.put(numOfVariables, varName);
        this.coefficients.put(varName, numOfVariables);
        this.numOfVariables += 1;
        coefficients.add(null);
        this.c.add(BigDecimal.ZERO);
      }
      String t = tokenMatcher.group(2);
      t = t.replaceAll("\\s+", "");
      if (t.compareTo("") == 0 || t.compareTo("+") == 0) {
        t = "1";
      } else if (t.compareTo("-") == 0) {
        t = "-1";
      }
      coefficients.set(this.coefficients.get(varName), new BigDecimal(t));
    }
    Matcher inequalitySignMatcher = inequalitySignPattern.matcher(constraint);
    if (inequalitySignMatcher.find()) {
      String inequalitySign = inequalitySignMatcher.group(1);
      Matcher constraintNumberMatcher = constraintNumberPattern.matcher(constraint);
      if (constraintNumberMatcher.find()) {
        String constraintNumberStr = constraintNumberMatcher.group(2);
        constraintNumber = new BigDecimal(constraintNumberStr);
      } else {
        throw new SolutionException("No numeric value after constraint sign");
      }
      if (inequalitySign.compareTo(">=") == 0) {
        for (int i = 0; i < coefficients.size(); i++) {
          coefficients.set(i, coefficients.get(i).negate());
        }
        b.add(constraintNumber.negate());
        A.add(coefficients);
        this.numOfInequalities += 1;
      } else if (inequalitySign.compareTo("==") == 0 || inequalitySign.compareTo("=") == 0) {
        ArrayList<BigDecimal> auxiliaryCoefficients = (ArrayList<BigDecimal>) coefficients.clone();
        for (int i = 0; i < coefficients.size(); i++) {
          auxiliaryCoefficients.set(i, coefficients.get(i).negate());
        }
        A.add(coefficients);
        A.add(auxiliaryCoefficients);
        b.add(constraintNumber);
        b.add(constraintNumber.negate());
        this.numOfInequalities += 2;
      } else {
        A.add(coefficients);
        b.add(constraintNumber);
        this.numOfInequalities += 1;
      }
      return true;
    } else {
      return false;
    }
  }

  public LPStandardForm getLPStandardForm() {
    return this.stForm;
  }
}
