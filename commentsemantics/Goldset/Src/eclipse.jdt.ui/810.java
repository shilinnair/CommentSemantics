package invalidSelection;

public class A_test044 {

    public void foo() {
        /*]*/
        for (int i = 0; i < 10; i++) /*[*/
        foo();
    }
}
