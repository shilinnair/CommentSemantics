package p;

class A implements I {

    public void m() {
    }

    public void m1() {
    }

    void f(Inter i) {
        I a = new A();
        i.work(a);
    }
}
