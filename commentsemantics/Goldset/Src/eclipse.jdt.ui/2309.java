package validSelection;

public class A_test145_ {

    boolean flag;

    public boolean foo() {
        /*]*/
        do {
            if (flag)
                break;
            return false;
        } while (/*[*/
        flag);
        return true;
    }
}
