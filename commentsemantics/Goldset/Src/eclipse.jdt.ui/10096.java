package p;

class A {

    void f(boolean flag) {
        for (int i = 0; i < 10; i++) {
            f(i == 1);
        }
    }
}
