package branch_out;

public class A_test751 {

    public volatile boolean flag;

    public void foo() {
        extracted();
    }

    protected void extracted() {
        /*[*/
        for (int i = 0; i < 10; i++) {
            if (flag)
                break;
        /*]*/
        }
    }
}
