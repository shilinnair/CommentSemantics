//8, 32, 8, 44
package p;

import java.util.Collection;

class A {

    private void test(Collection c) {
        for (Object o : c) System.out.println(o.toString());
    }
}
