package lpsolver

import spock.lang.Specification
import spock.lang.Unroll

class LPSolverSpec extends Specification {

  def "initialize simplex invocation"() {

  }

  @Unroll
  def "minimum in b"() {
    given:
    def solver = new LPSolver()
    expect:
    solver.minInB(b as BigDecimal[]) == answer
    where:
    b                       || answer
    [1]                     || 0
    [1, 0, -1]              || 2
    [1, 1, 1, 2]            || 0
    [-1, -1000, -10, -1001] || 3
    []                      || -1
  }

  @Unroll
  def "get name for x0 variables"() {
    given:
    def solver = new LPSolver()
    def x0 = solver.getNameForX0(coefficiets)
    expect:
    !coefficiets.containsKey(x0)
    where:
    coefficiets                          | _
    [x1: 0, x2: 1, x3: 2]                | _
    [x0: 0, x1: 1]                       | _
    ["auxVar": 0, "x0": 1, "auxVar1": 2] | _
  }

  def "conversion into auxiliary lp"() {
    given:
    BigDecimal[][] A = [[1, 2, 3, 4, 5], [5, 4, 3, 2, 1], [1, 2, 3, 4, 5], [5, 4, 3, 2, 1], [1, 2, 3, 4, 5]]
    BigDecimal[][] resA = [[1, 2, 3, 4, 5, -1], [5, 4, 3, 2, 1, -1], [1, 2, 3, 4, 5, -1], [5, 4, 3, 2, 1, -1], [1, 2, 3, 4, 5, -1]]
    BigDecimal[] b = [1, 2, 3, 4, 5]
    BigDecimal[] c = [1, 2, 3, 4, 5]
    BigDecimal[] resC = [0, 0, 0, 0, 0, -1]
    HashMap<Integer, String> variables = [] as HashMap<Integer, String>
    HashMap<String, Integer> coefficients = [] as HashMap<String, Integer>
    def form = new LPStandardForm(A, b, c, variables, coefficients, 5, 5, true)
    def solver = new LPSolver()
    solver.getNameForX0(coefficients) >> "x0"
    when:
    def pair = solver.convertIntoAuxLP(form)
    def resState = pair.getLeft()
    then:
    resState.A == resA
    Arrays.equals(resState.c, resC)
    coefficients.containsKey("x0")
    variables.containsValue("x0")
  }

  def "conversion into slack form"() {
    given:
    def form = Mock(LPStandardForm)
    HashMap<Integer, String> variables = [0: "x0", 1: "x1", 2: "x4", 3: "x6"]
    HashMap<String, Integer> coefficients = ["x0": 0, "x1": 1, "x4": 2, "x6": 3]
    form.variables = variables
    form.coefficients = coefficients
    form.m = 4
    form.n = 4
    LPSolver solver = new LPSolver()
    def state = solver.convertIntoSlackForm(form)
    expect:
    state.variables.size() == 8
    state.coefficients.size() == 8
  }
}