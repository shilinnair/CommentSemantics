package p;

class A implements I {

    int x;
}

class ST {

    A[] supercs;
}

class T extends ST {

    void add(A c) {
        super.supercs[0] = c;
        super.supercs[0].x = 0;
    }
}
