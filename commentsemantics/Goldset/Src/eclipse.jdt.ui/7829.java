// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestClassN extends SuperClassN implements SuperInterfaceN {

    public static final FI fi =  x -> x++;

    {
        // [1]
        bar1(0, fi);
        // [2]
        bar2(0, fi);
        // [3]
        bar3(0, fi, 1);
    }

    void bar1(int x, FI fi) {
    }

    void bar2(int x, FI fi) {
    }

    void bar3(int i, FI fi, int j) {
    }

    void bar3(int i, String s, int j) {
    }
}

class SuperClassN {

    private void bar1(int i, FX fx) {
    }
}

interface SuperInterfaceN {

    static void bar2(int i, FX fx) {
    }

    ;
}

@FunctionalInterface
interface FI {

    int foo(int x);
}

@FunctionalInterface
interface FX {

    int foo(String s);
}
