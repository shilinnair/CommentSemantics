// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestClass extends SuperClass {

    public static final FI fi = TestClass::<>m;

    private static int m(int x) {
        return x++;
    }

    {
        // [1]
        bar(0, fi);
        // [2]
        super.bar(0, fi);
    }

     TestClass() {
        this(// [3]
        0, // [3]
        fi);
    }

     TestClass(int i, FI a) {
        super(// [4]
        i, // [4]
        fi);
    }

     TestClass(int i, FX b) {
    }

    {
        // [5]
        new TestClass(0, fi);
    }

    void bar(int x, FX fx) {
        System.out.println();
    }
}

class SuperClass {

    public  SuperClass() {
    }

     SuperClass(int i, FI fi) {
    }

     SuperClass(int x, FX fx) {
    }

    void bar(int i, FI fi) {
    }

    void bar(int x, FX fx) {
    }
}

@FunctionalInterface
interface FI {

    int foo(int x);
}

@FunctionalInterface
interface FX {

    int foo(String s);
}
