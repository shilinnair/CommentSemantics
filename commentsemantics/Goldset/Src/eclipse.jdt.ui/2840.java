package destination_in;

public class A_test1062 {

    static class B {

        class C {

            int foo() {
                return extracted();
            }
        }
    }

    protected static int extracted() {
        /*[*/
        return /*]*/
        0;
    }
}
