package expression_in;

public class TestConditionalExpression {

    int i(Object s, int k) {
        return k == 3 ? s.hashCode() : 3;
    }

    void f(int p) {
        int /*]*/
        u = i(this, /*[*/
        p);
    }
}
