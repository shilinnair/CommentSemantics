package operator_in;

public class TestPrefixPlus {

    int result;

    public void foo() {
        int i = 10;
        result/*]*/
         = inline(/*[*/
        ++i);
    }

    public int inline(int x) {
        return 1 + x;
    }
}
