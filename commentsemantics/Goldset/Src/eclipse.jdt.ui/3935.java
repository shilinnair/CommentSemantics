package duplicates_in;

public class A_test961 {

    private Object fO;

    public void foo(Object o) {
        /*[*/
        /*]*/
        fO = o;
    }

    public void bar(Object x) {
        foo(x);
        fO = x;
    }
}
