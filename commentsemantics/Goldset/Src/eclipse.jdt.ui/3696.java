package bugs_in;

public class Test_314407 {

    Object getImage(String s) {
        return null;
    }

    void foo() {
        Object /*]*/
        x = getImage();
        Object y = getImage("");
        Object z = getImage(x);
    }
}
