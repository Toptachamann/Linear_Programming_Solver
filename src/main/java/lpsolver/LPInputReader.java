package lpsolver;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
  private static final Pattern constraintPattern;
  private static final Logger logger = LogManager.getLogger(LPInputReader.class);

  static {
    objectivePattern = Pattern.compile("^((\\s*[+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))+\\s*$");
    constraintPattern =
        Pattern.compile(
            "^((\\s*[+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))+\\s*(=|==|<=|>=)\\s*(-?\\s*\\d+(\\.\\d+)?)\\s*$");
    tokenPattern = Pattern.compile("(([+-]?\\s*\\d*\\.?\\d*)\\*?([a-zA-Z]+\\d*))");
  }

  private ArrayList<ArrayList<BigDecimal>> A;
  private ArrayList<BigDecimal> b, c;
  private HashMap<Integer, String> variables;
  private HashMap<String, Integer> coefficients;
  private int numOfVariables, numOfInequalities;

  public LPInputReader() {}

  private void reload() {
    this.A = new ArrayList<>();
    this.b = new ArrayList<>();
    this.c = new ArrayList<>();
    this.variables = new HashMap<>();
    this.coefficients = new HashMap<>();
    this.numOfVariables = 0;
    this.numOfInequalities = 0;
  }

  @Contract("null -> fail")
  public LPStandardForm readLP(@NotNull File file) throws LPException, IOException {
    logger.trace("Start reading linear program from {}", file);
    if (!file.isFile()) {
      logger.error("{} is not a file file", file);
      throw new IllegalArgumentException();
    }
    if (!file.canRead()) {
      logger.error("{} should be a readable file", file);
      throw new IllegalArgumentException();
    }
    reload();
    boolean maximized;
    int constraintCounter = 0;
    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String maxOrMin = in.readLine();
      if (maxOrMin == null) {
        logger.error("Input file is empty");
        throw new LPException("Input file is empty");
      }
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
          logger.error("No constraints in the input file");
          throw new LPException("No constraints in the input file");
        }
      }
    }
    normalizeConstraintMatrix();
    logger.info(
        "Linear program has {} inequalities and {} variables", numOfInequalities, numOfVariables);
    return new LPStandardForm(
        A, b, c, variables, coefficients, numOfInequalities, numOfVariables, maximized);
  }

  @Contract("null -> fail")
  public LPStandardForm readLP(@NotNull String lp) throws LPException {
    logger.trace("Start reading lp {}", lp);
    reload();
    String[] lines = lp.split("\n");
    if (lines.length < 3) {
      logger.error("Linear program {} is incomplete", lp);
      throw new LPException("Incomplete lp");
    }
    boolean maximized = processMaxMinParam(lines[0]);
    c = processObjective(lines[1]);
    for (int i = 2; i < lines.length; i++) {
      processConstraint(lines[i]);
    }
    normalizeConstraintMatrix();
    logger.info(
        "Linear program has {} inequalities and {} variables", numOfInequalities, numOfVariables);
    return new LPStandardForm(
        A, b, c, variables, coefficients, numOfInequalities, numOfVariables, maximized);
  }

  @Contract("null -> fail")
  private boolean processMaxMinParam(@NotNull String maxOrMin) throws LPException {
    logger.trace("Processing max\\min parameter: {}", maxOrMin);
    maxOrMin = maxOrMin.trim();
    if (maxOrMin.equalsIgnoreCase("min")) {
      return false;
    } else if (maxOrMin.equalsIgnoreCase("max")) {
      return true;
    } else {
      logger.error("Incorrect max/min parameter");
      throw new LPException("Incorrect max/min parameter");
    }
  }

  @Contract("null -> fail")
  private ArrayList<BigDecimal> processObjective(@NotNull String objective) throws LPException {
    logger.trace("Processing objective {}", objective);
    if (!Pattern.matches(objectivePattern.pattern(), objective)) {
      logger.error("Can't recognize objective {}", objective);
      throw new LPException("Can't recognize objective");
    }
    ArrayList<BigDecimal> objectiveCoefficients = new ArrayList<>();
    Matcher tokenMatcher = tokenPattern.matcher(objective);
    String t;
    for (int i = 0; tokenMatcher.find(); i++) {
      String variableName = tokenMatcher.group(3);
      variables.put(i, variableName);
      coefficients.put(variableName, i);
      t = tokenMatcher.group(2).trim();
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

  @Contract("null -> fail")
  private void processConstraint(@NotNull String constraint) throws LPException {
    logger.trace("Processing constraint {}", constraint);
    Matcher constraintMatcher = constraintPattern.matcher(constraint);
    if (!constraintMatcher.find()) {
      logger.error("Can't recognize constraint {}", constraint);
      throw new LPException("Can't recognize constraint");
    }
    ArrayList<BigDecimal> coefficients = new ArrayList<>(numOfVariables + 1);
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
        c.add(BigDecimal.ZERO);
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

    String inequalitySign = constraintMatcher.group(4).trim();
    BigDecimal constraintNumber = new BigDecimal(constraintMatcher.group(5).replaceAll("\\s", ""));
    if (inequalitySign.compareTo(">=") == 0) {
      for (int i = 0; i < coefficients.size(); i++) {
        coefficients.set(i, coefficients.get(i).negate());
      }
      b.add(constraintNumber.negate());
      A.add(coefficients);
      this.numOfInequalities += 1;
    } else if (inequalitySign.compareTo("==") == 0 || inequalitySign.compareTo("=") == 0) {
      ArrayList<BigDecimal> auxiliaryCoefficients = new ArrayList<>(coefficients);
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
  }

  private void normalizeConstraintMatrix() {
    logger.trace("Normalizing constraint matrix");
    for (ArrayList<BigDecimal> row : A) {
      int size = row.size();
      for (int j = 0; j < numOfVariables - size; j++) {
        row.add(BigDecimal.ZERO);
      }
    }
  }
}
