package p;

public class A<S> {

    void bar() {
        B<S> site = null;
        baz(site.f, site.f);
    }

    void baz(C filters, I fs) {
        filters.foo();
    }
}
