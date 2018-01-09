package lpsolver

import spock.lang.Specification

class LPInputReaderSpec extends Specification {

  def "Simple lp reading test"() {
    def lp = "max\nx1 + x2\nx1 + x2 <= 0"
    BigDecimal[][] A = [[1, 1]]
    BigDecimal[] b = [0]
    BigDecimal[] c = [1, 1]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1]
    def reader = new LPInputReader()
    def form = reader.readLP(lp)
    expect:
    form.A == A
    form.b == b
    form.c == c
    form.variables == variables
    form.coefficients == coefficients
    form.m == 1
    form.n == 2
    form.maximize
  }

  def "Complicated lp reading"() {
    def lp = """min
        782343246437439743943794343944324324324*x1  + 5273392392323.238324379948439874973439732242x2       
          6.338203729*x1   +   0.732932323x2 >=  9    
     -   102333.233232x1 + 2332.33214*x2 ==   13    
      11x1  -  x2   =  -   5435377467645646394439874397439347934734   """
    BigDecimal[][] A = [[-6.338203729, -0.732932323], [-102333.233232, 2332.33214], [102333.233232, -2332.33214],
                        [11, -1], [-11, 1]]
    BigDecimal[] b = [-9, 13, -13, -5435377467645646394439874397439347934734, 5435377467645646394439874397439347934734]
    BigDecimal[] c = [782343246437439743943794343944324324324, 5273392392323.238324379948439874973439732242]
    HashMap<Integer, String> variables = [0: "x1", 1: "x2"]
    HashMap<String, Integer> coefficients = [x1: 0, x2: 1]
    def reader = new LPInputReader()
    def form = reader.readLP(lp)
    expect:
    form.A == A
    form.b == b
    form.c == c
    form.variables == variables
    form.coefficients == coefficients
    form.m == 5
    form.n == 2
    !form.maximize
  }


}
