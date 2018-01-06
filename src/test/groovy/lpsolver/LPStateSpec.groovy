package lpsolver

import spock.lang.Specification
import spock.lang.Unroll

class LPStateSpec extends Specification {

  @Unroll
  def "test of getEntering method"() {
    given:
    BigDecimal[][] A = [[]]
    BigDecimal[] b = []
    BigDecimal v = 0
    HashMap<Integer, String> variables = new HashMap<>()
    HashMap<String, Integer> coefficients = new HashMap<>()
    def state = new LPState(A, b, c as BigDecimal[], v, variables, coefficients, c.size(), 0)
    expect:
    state.getEntering() == entering
    where:
    c               || entering
    [1, 2, 3]       || 0
    [0, 1, 2]       || 1
    [0, 0, 0, 0, 1] || 4
    [-1, -1, -1, 3] || 3
    [-1, 0, 0, 4]   || 3
  }

  def "test getting of leaving variable"() {
    given:
    BigDecimal[][] A = [[1, -1, 0, -1], [2, -2, 0.1, -1], [3, -2, 0, -4], [4, 1, 0, 0.1]]
    BigDecimal[] b = [1, 2, 3, 4]
    BigDecimal[] c = []
    HashMap<Integer, String> variables = new HashMap<>()
    HashMap<String, Integer> coefficients = new HashMap<>()
    BigDecimal v = 0
    def state = new LPState(A, b, c, v, variables, coefficients, 4, 4)
    expect:
    state.getLeaving(entering as int) == value
    where:
    entering || value
    0        || 0
    1        || 3
    2        || 1
    3        || 3
  }

  def "simple pivot test[1]"() {
    given:
    BigDecimal[][] A = [[1]]
    BigDecimal[] b = [5]
    BigDecimal[] c = [1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1]
    def state = new LPState(A, b, c, variables, coefficients, 1, 1)
    when:
    state.pivot(0, 0)
    then:
    state.A == [[1]] as BigDecimal[][]
    Arrays.equals(state.b, [5] as BigDecimal[])
    Arrays.equals(state.c, [-1] as BigDecimal[])
    state.v == 5G
    state.variables == [0: "x2", 1: "x1"]
    state.coefficients == [x2: 0, x1: 1]
  }

  @Unroll
  def "test pivot[2]"() {
    given:
    BigDecimal[][] A = [[1, 2], [4, 4]]
    BigDecimal[] b = [2, 4]
    BigDecimal[] c = [4, 2]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3", 3: "x4"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1, x3: 2, x4: 3]
    def state = new LPState(A, b, c, variables, coefficients, 2, 2)
    when:
    state.pivot(entering, leaving)
    then:
    state.A == resA as BigDecimal[][]
    Arrays.equals(state.b, resB as BigDecimal[])
    Arrays.equals(state.c, resC as BigDecimal[])
    5 == a
    state.v == resV
    state.variables == resVariables
    state.coefficients == resCoefficients
    where:
    leaving | entering || resV || resB    || resC
    0       | 0        || 8.0G || [2, -4] || [-4, -6]
    0       | 1        || 2.0G || [1, 0]  || [3.0, -1]
    1       | 0        || 4.0G || [1, 1]  || [-1, -2]
    1       | 1        || 2.0G || [0, 1]  || [2, -0.5]

    resA << [[[1, 2], [-4, -4]], [[0.5, 0.5], [2, -2]], [[-0.25, 1], [0.25, 1]], [[-1, -0.5], [1, 0.25]]]
    resVariables << [[0: "x3", 1: "x2", 2: "x1", 3: "x4"], [0: "x1", 1: "x3", 2: "x2", 3: "x4"],
                     [0: "x4", 1: "x2", 2: "x3", 3: "x1"], [0: "x1", 1: "x4", 2: "x3", 3: "x2"]]
    resCoefficients << [[x1: 2, x2: 1, x3: 0, x4: 3], [x1: 0, x2: 2, x3: 1, x4: 3],
                        [x1: 3, x2: 1, x3: 2, x4: 0], [x1: 0, x2: 3, x3: 2, x4: 1]]
  }

}
