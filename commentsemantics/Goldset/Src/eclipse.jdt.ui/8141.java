package operator_in;

public class TestPlusPrefix {

    int result;

    public void foo() {
        int i = 10;
        result/*]*/
         = inline(10 + /*[*/
        10);
    }

    public int inline(int x) {
        return ++x;
    }
}
