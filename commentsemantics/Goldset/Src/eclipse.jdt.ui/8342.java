package p;

import java.util.Collections;
import java.util.Map;

class A extends Sup<String> {

    public  A(Map<String, Integer> map) {
        method(Collections.<String, Integer>emptyMap());
        method2(Collections.<String, Integer>emptyMap());
        method3(Collections.<String, Integer>emptyMap());
        new A(Collections.<String, Integer>emptyMap());
        super.sup(Collections.<String, Integer>emptyMap());
        Map<String, Integer> emptyMap2 = Collections.emptyMap();
        emptyMap2 = Collections.emptyMap();
        Object o = Collections.emptyMap();
        Map<? extends String, ?> emptyMap3 = Collections.emptyMap();
        Integer first = Collections.<String, Integer>emptyMap().values().iterator().next();
    }

    void method(Map<String, Integer> map) {
    }

    void method2(Map<? extends String, ?> map) {
    }

    void method3(Object map) {
    }
}

class Sup<S> {

    <A extends S, B> void sup(Map<A, B> map) {
    }
}
