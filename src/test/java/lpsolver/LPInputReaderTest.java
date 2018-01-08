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
}
