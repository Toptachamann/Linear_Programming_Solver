package lpsolver

import spock.lang.Specification
import spock.lang.Unroll

class LPSolverSpec extends Specification {

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
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3", 3: "x4", 4: "x5"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1, x3: 2, x4: 3, x5: 4]
    def form = new LPStandardForm(A, b, c, variables, coefficients, 5, 5, true)
    def solver = new LPSolver()
    solver.getNameForX0(coefficients) >> "x0"
    when:
    def resState = solver.convertIntoAuxLP(form)
    then:
    resState.coefficients.size() == resState.variables.size()
    resState.A == resA
    resState.c == resC
    resState.coefficients.containsKey("x0")
    resState.variables.containsValue("x0")
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
    form.hasVariableNames() >> true
    LPSolver solver = new LPSolver()
    def state = solver.convertIntoSlackForm(form)
    expect:
    state.variables.size() == 8
    state.coefficients.size() == 8
  }

  def "lp solving [1]"() {
    BigDecimal[][] A = [[4, -1], [2, 1], [-5, 2]]
    BigDecimal[] b = [8, 10, 2]
    BigDecimal[] c = [1, 1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1, x3: 2]
    def form = new LPStandardForm(A, b, c, variables, coefficients, 3, 2, true)
    def solver = new LPSolver()
    def ans = solver.solve(form)
    expect:
    ans == 8 as BigDecimal
  }

  def "minimization"() {
    BigDecimal[][] A = [[1, -4], [1, -1], [1, 1]]
    BigDecimal[] b = [0, 3, 11]
    BigDecimal[] c = [-3, 1]
    def form = new LPStandardForm(A, b, c, 3, 2, false)
    def solver = new LPSolver()
    def ans = solver.solve(form)
    expect:
    ans == -17
  }

  def "initial infeasible solution"() {
    BigDecimal[][] A = [[1, 0], [-1, 0], [0, 1], [0, -1]]
    BigDecimal[] b = [10, -2, 10, -2]
    BigDecimal[] c = [1, 1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1]
    def form = new LPStandardForm(A, b, c, variables, coefficients, 4, 2, true)
    def solver = new LPSolver()
    def ans = solver.solve(form)
    expect:
    ans == 20
  }

  def "auxiliary lp solving"() {
    BigDecimal[][] A = [[1, 0, -1], [-1, 0, -1], [0, 1, -1], [0, -1, -1]]
    BigDecimal[] b = [10, -2, 10, -2]
    BigDecimal[] c = [0, 0, -1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x0", 3: "x3", 4: "x4", 5: "x5", 6: "x6"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1, x0: 2, x3: 3, x4: 4, x5: 5, x6: 6]
    def auxLP = new LPState(A, b, c, variables, coefficients, 4, 3)
    def solver = new LPSolver()
    solver.solveAuxLP(auxLP, 2, 1)
    expect:
    auxLP.v == 0
  }

  def "restoring initial lp"() {
    BigDecimal[][] A = [[0, -2, 1], [-1, 1, 0], [1, -2, 0], [0, 1, -1]]
    BigDecimal[] b = [8, 2, 8, 2]
    BigDecimal[] c = [0, -1, 0]
    HashMap<Integer, String> variables = [0: "x6", 1: "x0", 2: "x4", 3: "x3", 4: "x2", 5: "x5", 6: "x1"]
    HashMap<String, Integer> coefficients = [x6: 0, x0: 1, x4: 2, x3: 3, x2: 4, x5: 5, x1: 6]
    def auxLP = new LPState(A, b, c, BigDecimal.ZERO, variables, coefficients, 4, 3)

    BigDecimal[][] initA = [[]]
    BigDecimal[] initb = []
    BigDecimal[] initc = [1, 1]
    HashMap<Integer, String> initVariables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> initCoefficients = [x1: 0, x2: 1]
    def initial = new LPStandardForm(initA, initb, initc, initVariables, initCoefficients, 4, 2, true)
    def solver = new LPSolver()
    def result = solver.restoreInitialLP(auxLP, initial, 1)
    expect:
    result.A == [[0, 1], [-1, 0], [1, 0], [0, -1]] as BigDecimal[][]
    result.b == [8, 2, 8, 2] as BigDecimal[]
    result.c == [1, 1] as BigDecimal[]
    result.v == 4
    result.variables == [0: "x6", 1: "x4", 2: "x3", 3: "x2", 4: "x5", 5: "x1"]
    result.coefficients == [x6: 0, x4: 1, x3: 2, x2: 3, x5: 4, x1: 5]
  }

  def "unbounded linear program"() {
    given:
    BigDecimal[][] A = [[1, 0]]
    BigDecimal[] b = [1]
    BigDecimal[] c = [1, 1]
    def form = new LPStandardForm(A, b, c, 1, 2, true)
    def solver = new LPSolver()
    when:
    solver.solve(form)
    then:
    def e = thrown(SolutionException)
    e.getMessage() == "This linear program is unbounded"
  }

  def "unbounded with initial infeasible solution"() {
    given:
    BigDecimal[][] A = [[-1, 0], [0, 1], [0, -1]]
    BigDecimal[] b = [-2, 4, -2]
    BigDecimal[] c = [1, 1]
    def form = new LPStandardForm(A, b, c, 3, 2, true)
    def solver = new LPSolver()
    when:
    solver.solve(form)
    then:
    def e = thrown(LPException)
    e.getMessage() == "This linear program is unbounded"
  }

  @Unroll
  def "infeasible linear program, one variable"() {
    def form = new LPStandardForm(A as BigDecimal[][], b as BigDecimal[], c as BigDecimal[], m, n, true)
    def solver = new LPSolver()
    when:
    solver.solve(form)
    then:
    def e = thrown(LPException)
    e.getMessage() == "This linear program is infeasible"
    where:
    A           | b       | c      | m | n
    [[1], [-1]] | [0, -1] | [1]    | 2 | 1
    [[1, 1]]    | [-1]    | [1, 1] | 1 | 2
  }

}