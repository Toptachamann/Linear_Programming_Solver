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

    BigDecimal[] c1 = [1, 2, 3]
    BigDecimal[] c2 = [0, 1, 2]
    BigDecimal[] c3 = [0, 0, 0, 0, 1]
    BigDecimal[] c4 = [-1, -1, -1, 3]
    BigDecimal[] c5 = [-1, 0, 0, 4]
    def state1 = new LPState(A, b, c1, v, variables, coefficients, 0, 3)
    def state2 = new LPState(A, b, c2, v, variables, coefficients, 0, 3)
    def state3 = new LPState(A, b, c3, v, variables, coefficients, 0, 5)
    def state4 = new LPState(A, b, c4, v, variables, coefficients, 0, 4)
    def state5 = new LPState(A, b, c5, v, variables, coefficients, 0, 4)
    expect:
    state1.getEntering() == 0
    state2.getEntering() == 1
    state3.getEntering() == 4
    state4.getEntering() == 3
    state5.getEntering() == 3
  }

  def "test getting of leaving variable"(){
    given:
    BigDecimal[][] A = [[1,-1,0,-1], [2,-2,0.1,-1], [3,-2,0,-4], [4,1,0,0.1]]
    BigDecimal[] b = [1,2,3,4]
    BigDecimal[] c = []
    HashMap<Integer, String> variables = new HashMap<>()
    HashMap<String, Integer> coefficients = new HashMap<>()
    BigDecimal v = 0
    def state = new LPState(A, b, c, v, variables, coefficients, 4, 4)
    expect:
    state.getLeaving(entering as int) == value

    where:
    entering  | value
    0         | 0
    1         | 3
    2         | 1
    3         | 3
  }
}
