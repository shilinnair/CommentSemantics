package p;

class A implements I {

    int x;
}

class T {

    A[] cs;

    void add(A c) {
        cs[0] = c;
    }

    void f() {
        cs[0].x = 0;
    }
}
