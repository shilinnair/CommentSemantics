package validSelection;

public class A_test146_ {

    boolean flag;

    public boolean foo() {
        while (flag) {
            /*]*/
            for (int i = 0; i < 10; i++) {
                if (flag)
                    break;
            }
            if (flag)
                break;
            return /*[*/
            false;
        }
        return true;
    }
}
