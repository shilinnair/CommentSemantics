package p;

//change to B
class B {
}

class A extends B {
}

class Test {

    void f() {
        A a = new A();
        f(a);
    }

    void f(B b) {
    }
}
