package lpsolver;

import spock.lang.Specification
import spock.lang.Unroll


class LPSolverSpec extends Specification {


    @Unroll
    def "test something: we should expect result = #result when x = #x "() {
        given:
        def a = 5

        expect:
        a * x == result

        where:
        x   ||   result
        1   ||   5
        2   ||   10
        3   ||   14
        4   ||   20
        5   ||   25
    }

}