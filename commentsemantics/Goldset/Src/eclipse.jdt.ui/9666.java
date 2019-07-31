package p;

class A1 implements A {

    /**
	 * @deprecated Use {@link #m(boolean,int)} instead
	 */
    public void m(int i, boolean b) {
        m(b, i);
    }

    public void m(boolean b, int i) {
    }

    private void foo() {
        m(true, 2);
    }
}

class B extends A1 {

    public void m(int j, boolean b) {
        m(b, j);
    }

    public void m(boolean b, int j) {
        m(false, 6);
        super.m(true, 4);
    }
}

interface A {

    public void m(int i, boolean b);

    public void m(boolean b, int i);
}
