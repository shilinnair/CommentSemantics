//renaming A to B
package p;

public class B {

    static B A;
}

class X extends p.B {

    void x() {
        //fields come first
        p.B.A = A.A;
    }
}
