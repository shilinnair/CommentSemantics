package invalidSelection;

public class A_test080 {

    public boolean fBoolean;

    public void foo() {
        /*]*/
        if (fBoolean/*[*/
        )
            foo();
        else
            foo();
    }
}
