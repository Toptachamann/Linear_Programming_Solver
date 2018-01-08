package lpsolver

import spock.lang.Specification

class LPStandardFormSpec extends Specification {
  def "test getDual"() {
    given:
    BigDecimal[][] A = [[1, -2, -1, 3], [3, 1, 0, 4], [3, 4, 2, 2]]
    BigDecimal[] b = [3, 4, 1]
    BigDecimal[] c = [4, 1, 2, 3]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3", 3: "x4"]
    HashMap<String, Integer> coefficients = ["x1": 0, "x2": 1, "x3": 2, "x4": 3]
    def form = new LPStandardForm(A, b, c, variables, coefficients, 3, 4, true)
    def dual = form.getDual()
    expect:
    dual.A == [[1, 3, 3], [-2, 1, 4], [-1, 0, 2], [3, 4, 2]] as BigDecimal[][]
    Arrays.equals(dual.b, c)
    Arrays.equals(dual.c, b)
    def coeffs = [0, 1, 2]
    variables.keySet().containsAll(coeffs)
    coefficients.values().containsAll(coeffs)
    dual.m == 4
    dual.n == 3
    !dual.maximize
  }
}
