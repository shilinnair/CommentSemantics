package return_out;

public class A_test710 {

    public boolean foo() {
        int i = 0;
        return extracted(i);
    }

    protected boolean extracted(int i) {
        /*[*/
        switch(i) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }
}
