package simple_in;

public class TestNestedCalls {

    public static String toInline(String s, String s2) {
        return s;
    }

    static class Inner {

        void bar() {
            System.out.println(TestNestedCalls.toInline("outer", TestNestedCalls.toInline("inner", "hello")));
        }
    }
}
