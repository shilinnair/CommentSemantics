package p;

class A {

    public static void main(String[] args) {
        new Generic<Number>().take(new Double(1));
        new Generic<Integer>().take(new Integer(2));
        new Impl().take(new Integer(3));
        new Impl().take(new Float(4));
        Generic<Number> gn = new Impl();
        gn.take(new Integer(6));
        gn.take(new Double(7));
    }
}

class Generic<G> {

    void take(/*target*/
    G g) {
        System.out.println("Generic#take(G): " + g);
    }
}

class Impl extends Generic<Number> {

    void take(Integer g) {
        System.out.println("nonripple Impl#take(Integer): " + g);
    }

    void take(/*ripple*/
    Number g) {
        System.out.println("Impl#take(Number): " + g);
    }
}
