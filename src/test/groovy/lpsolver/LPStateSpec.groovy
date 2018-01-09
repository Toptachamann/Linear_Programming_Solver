package lpsolver

import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class LPStateSpec extends Specification {

  @Unroll
  def "test of getEntering method"() {
    given:
    BigDecimal[][] A = [[]]
    BigDecimal[] b = []
    BigDecimal v = 0
    HashMap<Integer, String> variables = new HashMap<>()
    HashMap<String, Integer> coefficients = new HashMap<>()
    def state = new LPState(A, b, c as BigDecimal[], v, variables, coefficients, 0, c.size())
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
    state.v == resV
    state.variables == resVariables
    state.coefficients == resCoefficients
    where:
    leaving | entering || resB    || resC      || resV
    0       | 0        || [2, -4] || [-4, -6]  || 8.0G
    0       | 1        || [1, 0]  || [3.0, -1] || 2.0G
    1       | 0        || [1, 1]  || [-1, -2]  || 4.0G
    1       | 1        || [0, 1]  || [2, -0.5] || 2.0G

    resA << [[[1, 2], [-4, -4]], [[0.5, 0.5], [2, -2]], [[-0.25, 1], [0.25, 1]], [[-1, -0.5], [1, 0.25]]]
    resVariables << [[0: "x3", 1: "x2", 2: "x1", 3: "x4"], [0: "x1", 1: "x3", 2: "x2", 3: "x4"],
                     [0: "x4", 1: "x2", 2: "x3", 3: "x1"], [0: "x1", 1: "x4", 2: "x3", 3: "x2"]]
    resCoefficients << [[x1: 2, x2: 1, x3: 0, x4: 3], [x1: 0, x2: 2, x3: 1, x4: 3],
                        [x1: 3, x2: 1, x3: 2, x4: 0], [x1: 0, x2: 3, x3: 2, x4: 1]]
  }

  @Unroll
  def "test concurrent pivot 1"() {
    given:
    BigDecimal[][] A = [[1, 2, 4, 2, 2], [5, 5, 2, 1, 1], [2, 2, 1, 1, 4], [4, 2, 4, 1, 2]]
    BigDecimal[] b = [2, 1, 4, 2]
    BigDecimal[] c = [2, 4, 1, 5, 1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3", 3: "x4", 4: "x5", 5: "x6", 6: "x7", 7: "x8", 8: "x9"]
    HashMap<String, Integer> coefficients = ["x1": 0, "x2": 1, "x3": 2, "x4": 3, "x5": 4, "x6": 5, "x7": 6, "x8": 7, "x9": 8]
    def state = new LPState(A, b, c, variables, coefficients, b.size(), c.size())
    when:
    state.pivotConcurrently(entering, leaving)
    then:
    state.A == resA as BigDecimal[][]
    Arrays.equals(state.b, resB as BigDecimal[])
    Arrays.equals(state.c, resC as BigDecimal[])
    state.v == resV
    state.variables == resVariables
    state.coefficients == resCoefficients
    where:
    entering | leaving || resB              || resC               || resV
    0        | 0       || [2, -9, 0, -6]    || [-2, 0, -7, 1, -3] || 4
    2        | 2       || [-14, -7, 4, -14] || [0, 2, -1, 4, -3]  || 4

    resA << [[[1, 2, 4, 2, 2], [-5, -5, -18, -9, -9], [-2, -2, -7, -3, 0], [-4, -6, -12, -7, -6]],
             [[-7, -6, -4, -2, -14], [1, 1, -2, -1, -7], [2, 2, 1, 1, 4], [-4, -6, -4, -3, -14]]]
    resVariables << [[0: "x6", 1: "x2", 2: "x3", 3: "x4", 4: "x5", 5: "x1", 6: "x7", 7: "x8", 8: "x9"],
                     [0: "x1", 1: "x2", 2: "x8", 3: "x4", 4: "x5", 5: "x6", 6: "x7", 7: "x3", 8: "x9"]]
    resCoefficients << [[x1: 5, x2: 1, x3: 2, x4: 3, x5: 4, x6: 0, x7: 6, x8: 7, x9: 8],
                        [x1: 0, x2: 1, x3: 7, x4: 3, x5: 4, x6: 5, x7: 6, x8: 2, x9: 8]]

  }

  @Unroll
  def "test concurrent pivot 2"() {
    given:
    BigDecimal[][] A = [[2, 4], [7, 2], [5, 4], [1, 3], [4, 1], [6, 2], [1, 7]]
    BigDecimal[] b = [3, 6, 5, 10, 2, 1, 4]
    BigDecimal[] c = [4, 3]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2", 2: "x3", 3: "x4", 4: "x5", 5: "x6", 6: "x7", 7: "x8", 8: "x9"]
    HashMap<String, Integer> coefficients = ["x1": 0, "x2": 1, "x3": 2, "x4": 3, "x5": 4, "x6": 5, "x7": 6, "x8": 7, "x9": 8]
    def state = new LPState(A, b, c, variables, coefficients, b.size(), c.size())

    when:
    state.pivotConcurrently(entering, leaving)
    then:
    state.A == resA as BigDecimal[][]
    Arrays.equals(state.b, resB as BigDecimal[])
    Arrays.equals(state.c, resC as BigDecimal[])
    state.v == resV
    state.variables == resVariables
    state.coefficients == resCoefficients
    where:
    entering | leaving || resB                              || resC     || resV
    0        | 3       || [-17, -64, -45, 10, -38, -59, -6] || [-4, -9] || 40
    1        | 4       || [-5, 2, -3, 4, 2, -3, -10]        || [-8, -3] || 6

    resA << [[[-2, -2], [-7, -19], [-5, -11], [1, 3], [-4, -11], [-6, -16], [-1, 4]],
             [[-14, -4], [-1, -2], [-11, -4], [-11, -3], [4, 1], [-2, -2], [-27, -7]]]
    resVariables << [[0: "x6", 1: "x2", 2: "x3", 3: "x4", 4: "x5", 5: "x1", 6: "x7", 7: "x8", 8: "x9"],
                     [0: "x1", 1: "x7", 2: "x3", 3: "x4", 4: "x5", 5: "x6", 6: "x2", 7: "x8", 8: "x9"]]
    resCoefficients << [[x1: 5, x2: 1, x3: 2, x4: 3, x5: 4, x6: 0, x7: 6, x8: 7, x9: 8],
                        [x1: 0, x2: 6, x3: 2, x4: 3, x5: 4, x6: 5, x7: 1, x8: 7, x9: 8]]
  }

  def "test concurrent pivot invocation"() {
    given:
    int threshold = LPState.PARALLEL_THRESHOLD
    System.out.println(threshold)
    BigDecimal[][] A = new BigDecimal[threshold][threshold]
    BigDecimal[] b = new BigDecimal[threshold]
    BigDecimal[] c = new BigDecimal[threshold]
    Arrays.fill(A, Collections.nCopies(threshold, BigDecimal.ONE) as BigDecimal[])
    Arrays.fill(b, BigDecimal.ONE)
    Arrays.fill(c, BigDecimal.ONE)
    HashMap<Integer, String> variables = [0: "x1", (threshold): "x" + threshold]
    HashMap<String, Integer> coefficients = [x1: 0, ("x" + threshold): threshold]
    System.out.println(variables)
    System.out.println(coefficients)
    def state = Spy(LPState, constructorArgs: [A, b, c, variables, coefficients, threshold, threshold])
    when:
    state.pivot(0, 0)
    then:
    0 * state.pivotSequentially(0, 0)
    1 * state.pivotConcurrently(0, 0)
  }

  def "test sequential pivot invocation"() {
    given:
    BigDecimal[][] A = [[1]]
    BigDecimal[] b = [1]
    BigDecimal[] c = [1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1]
    def state = Spy(LPState, constructorArgs: [A, b, c, variables, coefficients, 1, 1])
    when:
    state.pivot(0, 0)
    then:
    1 * state.pivotSequentially(0, 0)
    0 * state.pivotConcurrently(0, 0)
  }

}
