package p;

class Outer {

    enum A implements  {

        GREEN() {
        }
        , DARK_GREEN() {
        }
        , BLACK() {
        }
        ;

        public A getNext(boolean forward) {
            switch(this) {
                case GREEN:
                    return DARK_GREEN;
                case DARK_GREEN:
                    return BLACK;
                case BLACK:
                    return GREEN;
                default:
                    return null;
            }
        }
    }

    {
        A a = A.GREEN.getNext(true);
    }
}
