package p;

//can't rename m to k - defined in subclass
class A implements I {

    public void m() {
    }
}

class B extends A {

    private static void k() {
    }
}

interface I {

    void m();
}
