package lpsolver;

import org.junit.gen5.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LPInputReaderTest {
  private LPInputReader reader;

  LPInputReaderTest() {
    reader = new LPInputReader();
  }

  @Test
  @DisplayName("Maximization/minimization parameter processing test")
  public void maxMinTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method maxOrMinMethod = reader.getClass().getDeclaredMethod("processMaxMinParam", String.class);
    maxOrMinMethod.setAccessible(true);
    String s = null;
    Throwable throwable =
        assertThrows(InvocationTargetException.class, () -> maxOrMinMethod.invoke(reader, s));
    assertEquals(IllegalArgumentException.class, throwable.getCause().getClass(), "Should be NPE");
    assertAll(
        "Maximization test",
        () -> assertTrue((boolean) maxOrMinMethod.invoke(reader, "max")),
        () -> assertTrue((boolean) maxOrMinMethod.invoke(reader, "MAX")),
        () -> assertTrue((boolean) maxOrMinMethod.invoke(reader, "mAx")));
    assertAll(
        "Minimization test",
        () -> assertFalse((boolean) maxOrMinMethod.invoke(reader, "min")),
        () -> assertFalse((boolean) maxOrMinMethod.invoke(reader, "MIN")),
        () -> assertFalse((boolean) maxOrMinMethod.invoke(reader, "mIn")));
  }

  @Test
  @DisplayName("Objective processing test")
  public void processObjectiveTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method reload = reader.getClass().getDeclaredMethod("reload");
    reload.setAccessible(true);
    reload.invoke(reader);
    Method processObjective = reader.getClass().getDeclaredMethod("processObjective", String.class);
    processObjective.setAccessible(true);

    String nul = null;
    String empty = "";
    String objective1 = "x1";
    String objective2 = "5*x1 + 1.23x3 + 5.32*vv";
    ArrayList<BigDecimal> objectiveArr1 = new ArrayList<>(List.of(new BigDecimal(1)));
    ArrayList<BigDecimal> objectiveArr2 =
        new ArrayList<>(List.of(new BigDecimal(5), new BigDecimal("1.23"), new BigDecimal("5.32")));
    Throwable exception1 =
        assertThrows(
            InvocationTargetException.class,
            () -> processObjective.invoke(reader, nul),
            "Should throw InvocationTargetException");
    Throwable exception2 =
        assertThrows(
            InvocationTargetException.class,
            () -> processObjective.invoke(reader, empty),
            "Should throw InvocationTargetException");
    assertEquals(
        IllegalArgumentException.class, exception1.getCause().getClass(), "Should throw NPE");
    assertEquals(
        IllegalArgumentException.class,
        exception2.getCause().getClass(),
        "Should throw IllegalArgumentException");
    assertEquals(
        objectiveArr1,
        processObjective.invoke(reader, objective1),
        () -> "Should be " + objectiveArr1.toString());
    assertEquals(
        objectiveArr2,
        processObjective.invoke(reader, objective2),
        () -> "Should be " + objectiveArr2.toString());
  }

  @Test
  @DisplayName("Constraint processing test")
  public void processConstraintTest()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Method reload = reader.getClass().getDeclaredMethod("reload");
    reload.setAccessible(true);
    reload.invoke(reader);
    Method processConstraint =
        reader.getClass().getDeclaredMethod("processConstraint", String.class);
    processConstraint.setAccessible(true);

    String nul = null;
    String empty = "";
    String withoutSign = " 5*x1 + 1.23x3 + 5.32*vv ";
    String withoutConstraint = " 5*x1 + 1.23x3 + 5.32*vv <=  ";
    Throwable exception1 =
        assertThrows(InvocationTargetException.class, () -> processConstraint.invoke(reader, nul));
    Throwable exception2 =
        assertThrows(
            InvocationTargetException.class, () -> processConstraint.invoke(reader, empty));
    Throwable exception3 =
        assertThrows(
            InvocationTargetException.class, () -> processConstraint.invoke(reader, withoutSign));
    Throwable exception4 =
        assertThrows(
            InvocationTargetException.class,
            () -> processConstraint.invoke(reader, withoutConstraint));

    assertAll(
        "Exception throwing test",
        () ->
            assertEquals(
                IllegalArgumentException.class, exception1.getCause().getClass(), "Should be NPE"),
        () ->
            assertEquals(
                IllegalArgumentException.class,
                exception2.getCause().getClass(),
                "Should be IllegalArgumentException"),
        () ->
            assertEquals(
                IllegalArgumentException.class,
                exception3.getCause().getClass(),
                "Should be IllegalArgumentException"),
        () ->
            assertEquals(
                IllegalArgumentException.class,
                exception4.getCause().getClass(),
                "Should be IllegalArgumentException"));
  }

  @Test
  @DisplayName("Exceptions of reading lp from string")
  public void testLpReadingExceptions() {
    String nulStr = null;
    String empty = "";
    String withoutMax = "x1 + x2\nx1 + x2 <= 0";
    String withoutObjective = "max\nx1 + x2 <= 0";
    String withoutConstraint = "max\nx1 + x2";
    assertThrows(IllegalArgumentException.class, () -> reader.readLP(nulStr), "null -> NPE");
    assertThrows(
        IllegalArgumentException.class, () -> reader.readLP(empty), "empty -> SolutionException");
    assertThrows(
        IllegalArgumentException.class,
        () -> reader.readLP(withoutMax),
        "Without max -> SolutionException");
    assertThrows(
        IllegalArgumentException.class,
        () -> reader.readLP(withoutObjective),
        "No objective -> SolutionException");
    assertThrows(
        IllegalArgumentException.class,
        () -> reader.readLP(withoutConstraint),
        "No constraints -> SolutionException");
  }

  @Test
  @DisplayName("Exceptions of reading lp from file")
  public void testReadingFromFile() {
    File nulFile = null;
    File empty = new File("");
    File disk = new File("C:\\");
    File folder = new File("C:\\Java_Projects");
    assertThrows(IllegalArgumentException.class, () -> reader.readLP(nulFile));
    assertThrows(IllegalArgumentException.class, () -> reader.readLP(empty));
    assertThrows(IllegalArgumentException.class, () -> reader.readLP(disk));
    assertThrows(IllegalArgumentException.class, () -> reader.readLP(folder));
  }

  @Test
  @DisplayName("Simple lp reading test")
  public void simpleTestLP() throws SolutionException {
    String lp = "max\n" + "x1 + x2\n" + "x1 + x2 <= 0";
    LPStandardForm stForm = reader.readLP(lp);
    ArrayList<BigDecimal> c = new ArrayList<>(List.of(BigDecimal.ONE, BigDecimal.ONE));
    ArrayList<ArrayList<BigDecimal>> A = new ArrayList<>(List.of(c));
    ArrayList<BigDecimal> b = new ArrayList<>(List.of(BigDecimal.ZERO));
    HashMap<String, Integer> coefficients = new HashMap<>(Map.of("x1", 0, "x2", 1));
    HashMap<Integer, String> variables = new HashMap<>(Map.of(0, "x1", 1, "x2"));
    assertAll(
        "Validating simple LP",
        () -> assertEquals(A, stForm.A, "A should be [1,1]"),
        () -> assertEquals(b, stForm.b, "b should be [0]"),
        () -> assertEquals(c, stForm.c, "c should be [1, 1]"),
        () ->
            assertEquals(
                coefficients,
                stForm.coefficients,
                "coefficients should be ['x1'->0, 'x2'->1]"),
        () ->
            assertEquals(variables, stForm.variables, "variables should be [0->'x1', 1->'x2'"),
        () -> assertEquals(2, stForm.m, "Num of vars should be 2"),
        () -> assertEquals(1, stForm.n, "Num of ineq should be 1"),
        () -> assertTrue(stForm.maximize, "Maximized should be true"));
  }

  @Test
  @DisplayName("Complicated lp reading")
  public void complicatedLpTest() throws SolutionException {
    String lp =
        "min\n"
            + "    782343246437439743943794343944324324324*x1  + 5273392392323.238324379948439874973439732242x2       \n"
            + "  6.338203729*x1   +   0.732932323x2 >=  9    \n"
            + " -   102333.233232x1 + 2332.33214*x2 ==   13    \n"
            + "   11x1  -  x2   =  -   5435377467645646394439874397439347934734   ";
    ArrayList<BigDecimal> row1 =
        new ArrayList<>(List.of(new BigDecimal("-6.338203729"), new BigDecimal("-0.732932323")));
    ArrayList<BigDecimal> row2 =
        new ArrayList<>(List.of(new BigDecimal("-102333.233232"), new BigDecimal("2332.33214")));
    ArrayList<BigDecimal> row3 =
        new ArrayList<>(List.of(new BigDecimal("102333.233232"), new BigDecimal("-2332.33214")));
    ArrayList<BigDecimal> row4 =
        new ArrayList<>(List.of(new BigDecimal("11"), new BigDecimal("-1")));
    ArrayList<BigDecimal> row5 =
        new ArrayList<>(List.of(new BigDecimal("-11"), new BigDecimal("1")));
    ArrayList<ArrayList<BigDecimal>> A = new ArrayList<>(List.of(row1, row2, row3, row4, row5));
    ArrayList<BigDecimal> b =
        new ArrayList<>(
            List.of(
                new BigDecimal("-9"),
                new BigDecimal("13"),
                new BigDecimal("-13"),
                new BigDecimal("-5435377467645646394439874397439347934734"),
                new BigDecimal("5435377467645646394439874397439347934734")));
    ArrayList<BigDecimal> c =
        new ArrayList<>(
            List.of(
                new BigDecimal("782343246437439743943794343944324324324"),
                new BigDecimal("5273392392323.238324379948439874973439732242")));
    HashMap<String, Integer> coefficients = new HashMap<>(Map.of("x1", 0, "x2", 1));
    HashMap<Integer, String> variables = new HashMap<>(Map.of(0, "x1", 1, "x2"));
    LPStandardForm stForm = reader.readLP(lp);
    assertAll(
        "Complicated LP",
        () -> assertEquals(A, stForm.A),
        () -> assertEquals(b, stForm.b),
        () -> assertEquals(c, stForm.c),
        () -> assertEquals(variables, stForm.variables),
        () -> assertEquals(coefficients, stForm.coefficients),
        () -> assertEquals(2, stForm.m),
        () -> assertEquals(5, stForm.n),
        () -> assertFalse(stForm.maximize));
  }
}
