package lpsolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LPStandardFormTest {
  private LPInputReader reader;

  public LPStandardFormTest() {
    reader = new LPInputReader();
  }

  @Test
  @DisplayName("Testing conversion into dual")
  public void testDual() throws SolutionException {
    String lp = "min\n" + "0.5*x1+3*x2+x3\n" + "x1+x2+x3<=40\n" + "-2*x1-x2+x3<=10\n" + "x2-x3<=10";
    LPStandardForm stForm = reader.readLP(lp);
    LPStandardForm dual = stForm.getDual();
    ArrayList<BigDecimal> row1 =
        new ArrayList<>(List.of(new BigDecimal("1"), new BigDecimal("-2"), new BigDecimal("0")));
    ArrayList<BigDecimal> row2 =
        new ArrayList<>(List.of(new BigDecimal("1"), new BigDecimal("-1"), new BigDecimal("1")));
    ArrayList<BigDecimal> row3 =
        new ArrayList<>(List.of(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("-1")));
    ArrayList<ArrayList<BigDecimal>> A = new ArrayList<>(List.of(row1, row2, row3));
    ArrayList<BigDecimal> b =
        new ArrayList<>(List.of(new BigDecimal("0.5"), new BigDecimal("3"), new BigDecimal("1")));
    ArrayList<BigDecimal> c =
        new ArrayList<>(List.of(new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("10")));
    HashMap<String, Integer> coefficients = new HashMap<>(Map.of("x1", 0, "x2", 1, "x3", 2));
    HashMap<Integer, String> variables = new HashMap<>(Map.of(0, "x1", 1, "x2", 2, "x3"));
    assertAll(
        "Testing dual",
        () -> assertEquals(A, dual.A),
        () -> assertEquals(b, dual.b),
        () -> assertEquals(c, dual.c),
        () -> assertEquals(variables, dual.variables),
        () -> assertEquals(coefficients, dual.coefficients),
        () -> assertEquals(3, dual.n),
        () -> assertEquals(3, dual.m),
        () -> assertTrue(dual.maximize));
  }
}
