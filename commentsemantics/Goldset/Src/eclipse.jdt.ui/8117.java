package p;

class PairDance {

    public static void main(String[] args) {
        InvertedPair ip = new InvertedPair();
        Pair /*<Double, Integer>*/
        p = ip;
        p.setA(new Double(1.1));
        Double a = (Double) ip.getA();
        ip.setB(new Integer(2));
        System.out.println(ip);
    }
}
