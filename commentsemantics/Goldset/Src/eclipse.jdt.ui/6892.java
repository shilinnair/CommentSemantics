package p;

class A implements I {

    int f;

    public void m() {
    }

    public void m1() {
    }

    protected A f() {
        return this;
    }

    void test() {
        f().f = 0;
    }
}
