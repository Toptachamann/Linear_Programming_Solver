package lpsolver;

import org.junit.gen5.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    assertEquals(NullPointerException.class, throwable.getCause().getClass(), "Should be NPE");
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
  public void processObjectiveTest() throws NoSuchMethodException {
    Method processObjective = reader.getClass().getDeclaredMethod("processObjective", String.class);
    processObjective.setAccessible(true);
    String s = null;
    Throwable exception1 =
        assertThrows(
            InvocationTargetException.class,
            () -> processObjective.invoke(reader, s),
            "Should throw InvocationTargetException");
    assertEquals(NullPointerException.class, exception1.getCause().getClass(), "Should throw NPE");
    String objective1 = "";
    String objective2 = "x1";
    String objective3 = "5*x1 + 1.23x3 + 5.32*vv";
    Throwable exception2 =
        assertThrows(
            InvocationTargetException.class,
            () -> processObjective.invoke(reader, objective1),
            "Should throw InvocationTargetException");
    assertEquals(
        IllegalAccessException.class,
        exception2.getCause().getClass(),
        "Should throw IllegalArgumentException");
    ArrayList<BigDecimal> objectiveArr1 = new ArrayList<>(List.of(new BigDecimal(1)));
  }
}
