package p;

public class DontMoveArgs2_in {

    private int fX, fY;

    public  DontMoveArgs2_in(/*]*/
    int x, int y) {
        fX = x;
        fY = y;
    }

    public int getX() {
        return fX;
    }

    public int getY() {
        return fY;
    }

    public static void main(String[] args) {
        int y = 20;
        DontMoveArgs2_in dma = new DontMoveArgs2_in(15, y);
        System.out.println("Value = " + Integer.toHexString(dma.getX() + dma.getY()));
    }
}
