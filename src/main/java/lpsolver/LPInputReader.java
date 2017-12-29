package lpsolver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;

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
  private static final Pattern tokenPattern;
  private static final Pattern objectivePattern;
  private static final Pattern inequalitySignPattern;
  private static final Pattern constraintNumberPattern;

  static {
    objectivePattern = Pattern.compile("((\\s*[+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))+");
    tokenPattern = Pattern.compile("(([+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))");
    inequalitySignPattern = Pattern.compile("(>=|<=|=|==)");
    constraintNumberPattern = Pattern.compile("(>=|<=|=|==)\\s*(-?\\d+(\\.?\\d+)?)");
  }

  private LPStandardForm stForm;
  private ArrayList<ArrayList<BigDecimal>> A;
  private ArrayList<BigDecimal> b, c;
  private HashMap<Integer, String> variables;
  private HashMap<String, Integer> coefficients;
  private int numOfVariables, numOfInequalities;

  public LPInputReader() {}

  private void reload() {
    this.stForm = null;
    this.A = new ArrayList<>();
    this.b = new ArrayList<>();
    this.c = new ArrayList<>();
    this.variables = new HashMap<>();
    this.coefficients = new HashMap<>();
    this.numOfVariables = 0;
    this.numOfInequalities = 0;
  }

  public void readLP(File file) throws SolutionException, IOException {
    Validate.isTrue(file.exists(), "File should exist");
    Validate.isTrue(file.isFile(), "File should be a file");
    Validate.isTrue(file.canRead(), "File should be readable");
    reload();
    boolean maximized;
    int constraintCounter = 0;
    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String maxOrMin = in.readLine();
      Validate.notNull(maxOrMin, "Input file is empty");
      maximized = processMaxMinParam(maxOrMin);

      String objective = in.readLine();
      this.c = processObjective(objective);
      String constraint;
      while ((constraint = in.readLine()) != null) {
        if (!StringUtils.isBlank(constraint)) {
          processConstraint(constraint);
          ++constraintCounter;
        } else if (constraintCounter > 0) {
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
    stForm = new LPStandardForm(A, b, c, coefficients, variables, numOfVariables, numOfInequalities, maximized);
  }

  public void readLP(String lp) throws IOException, SolutionException {
    Validate.notNull(lp, "LP should be not null");
    String[] lines = lp.split("\n");
    Validate.isTrue(lines.length >= 3, "Incomplete lp");

    boolean maximized = processMaxMinParam(lines[0]);
    processObjective(lines[1]);
    for(int i = 2; i < lines.length; i++) {
      processConstraint(lines[i]);
    }
  }

  private boolean processMaxMinParam(String maxOrMin) throws SolutionException, IOException {
    Validate.notNull(maxOrMin, "Parameter should be not null");
    maxOrMin = maxOrMin.trim();
    if(maxOrMin.equalsIgnoreCase("min")){
      return false;
    }else if(maxOrMin.equalsIgnoreCase("max")){
      return true;
    }else{
      throw new SolutionException("Incorrect max/min parameter");
    }
  }

  @Contract("null -> fail")
  private ArrayList<BigDecimal> processObjective(String objective) throws IOException {
    Validate.notNull(objective, "Objective should be not null");
    Matcher objectiveMatcher = objectivePattern.matcher(objective);
    Validate.isTrue(objectiveMatcher.matches(), "Can't recognize objective function");
    ArrayList<BigDecimal> objectiveCoefficients = new ArrayList<>();
    Matcher matcher = tokenPattern.matcher(objective);
    String t;
    for (int i = 0; matcher.find(); i++) {
      String variableName = matcher.group(3);
      variables.put(i, variableName);
      coefficients.put(variableName, i);
      t = matcher.group(2);
      t = t.replaceAll("\\s", "");
      if (t.compareTo("") == 0 || t.compareTo("+") == 0) {
        t = "1";
      } else if (t.compareTo("-") == 0) {
        t = "-1";
      }
      objectiveCoefficients.add(new BigDecimal(t));
    }
    numOfVariables = variables.size();
    return objectiveCoefficients;
  }

  private boolean processConstraint(String constraint) throws SolutionException {
    ArrayList<BigDecimal> coefficients = new ArrayList<>(numOfVariables + 1);
    BigDecimal constraintNumber;
    for (int i = 0; i < numOfVariables; i++) {
      coefficients.add(BigDecimal.ZERO);
    }
    Matcher tokenMatcher = tokenPattern.matcher(constraint);
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
