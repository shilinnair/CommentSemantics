package p;

abstract class A {

    public abstract void f();
}

abstract class B extends A {

    public abstract void f();
}

class C extends A {

    public void f() {
    }
}
